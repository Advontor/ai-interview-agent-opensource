"""Build the checked-in, read-only local interview knowledge resource.

Usage:
  python scripts/import_interview_rag.py <source.csv> [output.jsonl.gz]

The source is never written. The output is deterministic (gzip mtime is fixed)
so another developer can rebuild the same resource from the same CSV.
"""
from __future__ import annotations

import csv
import gzip
import hashlib
import json
import re
import sys
from pathlib import Path

REQUIRED = {"row_id", "question", "answer", "primary_job_tag", "job_tags",
            "skill_tags", "scene_tags", "question_type", "difficulty",
            "keywords", "retrieval_text"}
ROLE_MAP = {"后端开发": "后端开发", "前端开发": "前端开发",
            "产品经理": "产品经理", "通用面试": "通用面试"}
DIFFICULTY_MAP = {"basic": "EASY", "intermediate": "MEDIUM", "advanced": "HARD"}


def clean(value: str | None) -> str:
    return re.sub(r"\s+", " ", (value or "").replace("\u0000", "")).strip()


def tags(value: str | None) -> list[str]:
    return list(dict.fromkeys(clean(value).replace("，", "|").replace(",", "|").split("|"))) if clean(value) else []


def build(source: Path, output: Path) -> dict:
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    rows: list[dict] = []
    ids: set[str] = set()
    questions: set[str] = set()
    with source.open("r", encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        fields = set(reader.fieldnames or [])
        missing = REQUIRED - fields
        if missing:
            raise ValueError(f"CSV 缺少字段: {sorted(missing)}")
        for number, raw in enumerate(reader, 2):
            row_id = clean(raw.get("row_id"))
            question = clean(raw.get("question"))
            answer = clean(raw.get("answer"))
            if not row_id or not question or not answer:
                raise ValueError(f"第 {number} 行缺少 row_id/question/answer")
            if row_id in ids or question in questions:
                raise ValueError(f"第 {number} 行存在重复 row_id 或 question")
            ids.add(row_id)
            questions.add(question)
            primary = clean(raw.get("primary_job_tag"))
            role = ROLE_MAP.get(primary, "其他岗位")
            difficulty = DIFFICULTY_MAP.get(clean(raw.get("difficulty")).lower())
            if difficulty is None:
                raise ValueError(f"第 {number} 行难度无法映射: {raw.get('difficulty')!r}")
            # The source `keywords` column contains mechanically generated fragments.
            # Use the curated tag columns and rebuild retrieval text from clean fields.
            all_tags = list(dict.fromkeys(tags(raw.get("job_tags")) + tags(raw.get("skill_tags"))
                                      + tags(raw.get("scene_tags"))))
            # Keep answer for display/context, but use a bounded, reproducible retrieval text.
            retrieval = f"{question} {' '.join(all_tags[:30])} {answer[:800]}"[:1800]
            rows.append({
                "rowId": row_id,
                "question": question,
                "answer": answer,
                "role": role,
                "difficulty": difficulty,
                "tags": all_tags[:40],
                "retrievalText": retrieval,
                "questionType": clean(raw.get("question_type")),
            })
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("wb") as raw_output:
        # filename='' avoids machine-specific path metadata in the gzip header.
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw_output, mtime=0) as zipped:
            for row in rows:
                zipped.write((json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8"))
    meta = {"format": "interview-rag-v1", "sourceSha256": digest,
            "rowCount": len(rows), "sourceName": source.name}
    output.with_suffix(".meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return meta


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3):
        raise SystemExit("usage: import_interview_rag.py SOURCE.csv [OUTPUT.jsonl.gz]")
    source_path = Path(sys.argv[1]).resolve()
    output_path = Path(sys.argv[2]).resolve() if len(sys.argv) == 3 else Path("src/main/resources/knowledge/interview-rag-v1.jsonl.gz").resolve()
    print(json.dumps(build(source_path, output_path), ensure_ascii=False))
