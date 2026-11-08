package com.nanji.interview.ai_interview_system.modules.resume.service;

import com.nanji.interview.ai_interview_system.modules.resume.dto.Education;
import com.nanji.interview.ai_interview_system.modules.resume.dto.Internship;
import com.nanji.interview.ai_interview_system.modules.resume.dto.Project;
import com.nanji.interview.ai_interview_system.modules.resume.dto.ResumeDTO;
import com.nanji.interview.ai_interview_system.modules.resume.dto.ResumeScoreResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 简历规则评分器。
 *
 * 这里不再调用大模型，避免因为温度或模型版本变化导致同一简历得分漂移。
 * 解析字段可以由模型产生，但评分、归一化和自动填充建议全部在服务端完成。
 */
@Service
public class ResumeScoringService {

    public ResumeScoreResult score(ResumeDTO resume, String requestedRole) {
        ResumeDTO safe = resume == null ? new ResumeDTO() : resume;
        String targetRole = StringUtils.hasText(requestedRole)
                ? requestedRole.trim()
                : safe.getTargetRole();
        Set<String> techStack = collectTechStack(safe);

        int education = educationScore(safe.getEducationList());
        int internship = internshipScore(safe.getInternshipList());
        int projects = projectScore(safe.getProjectList());
        int skills = skillsScore(safe.getSkills());
        int stack = techStackScore(techStack);
        int roleMatch = roleMatchScore(safe, targetRole, techStack);
        int completeness = completenessScore(safe);

        ResumeScoreResult result = new ResumeScoreResult();
        result.getBreakdown().put("education", education);
        result.getBreakdown().put("internship", internship);
        result.getBreakdown().put("projects", projects);
        result.getBreakdown().put("skills", skills);
        result.getBreakdown().put("techStack", stack);
        result.getBreakdown().put("roleMatch", roleMatch);
        result.getBreakdown().put("completeness", completeness);

        // 权重固定并公开，前端可以直接解释分数来源。
        result.setTotalScore(clamp(Math.round(
                education * .15f
                        + internship * .15f
                        + projects * .20f
                        + skills * .15f
                        + stack * .15f
                        + roleMatch * .15f
                        + completeness * .05f)));

        result.getExplanations().put("education", educationExplanation(safe.getEducationList()));
        result.getExplanations().put("internship", internshipExplanation(safe.getInternshipList()));
        result.getExplanations().put("projects", projectExplanation(safe.getProjectList()));
        result.getExplanations().put("skills", "根据已识别的独立技能数量和技能描述完整度计算");
        result.getExplanations().put("techStack", "根据项目技术栈与技能字段去重后的技术栈覆盖情况计算");
        result.getExplanations().put("roleMatch", StringUtils.hasText(targetRole)
                ? "比较目标岗位与简历中的岗位、职位、技能和项目描述的匹配程度"
                : "未指定目标岗位，暂按可识别技术栈和经历推断岗位匹配度");
        result.getExplanations().put("completeness", "检查基本信息、时间、描述和成果字段是否完整");

        result.setTechStack(new ArrayList<>(techStack));
        buildInsights(result, safe, targetRole, techStack);
        result.setDefaults(buildDefaults(result, safe, targetRole, techStack));
        return result;
    }

    private void buildInsights(ResumeScoreResult result, ResumeDTO resume, String role, Set<String> stack) {
        if (result.getBreakdown().get("projects") >= 70) {
            result.getStrengths().add("项目经历数量和描述较完整");
        } else {
            result.getRisks().add("项目经历偏少或缺少可验证的成果描述");
        }
        if (result.getBreakdown().get("techStack") >= 60) {
            result.getStrengths().add("已识别出较清晰的技术栈：" + String.join("、", stack.stream().limit(8).toList()));
        } else {
            result.getRisks().add("技术栈信息不足，建议补充版本、使用场景和个人负责部分");
        }
        if (result.getBreakdown().get("internship") >= 65) {
            result.getStrengths().add("实习经历具备岗位相关性");
        } else {
            result.getRisks().add("实习经历较少，面试时应准备项目经验替代证明");
        }
        if (result.getBreakdown().get("completeness") < 60) {
            result.getRisks().add("部分时间、成果或基本信息缺失，建议在面试前补充");
        }

        if (!stack.isEmpty()) {
            result.getRecommendedTopics().addAll(stack.stream().limit(8).toList());
        }
        result.getRecommendedTopics().add("项目深挖");
        result.getRecommendedTopics().add("问题解决与技术取舍");
        if (resume.getInternshipList().isEmpty()) result.getRecommendedTopics().add("行为面试");

        if (StringUtils.hasText(role)) {
            result.getRecommendedRoles().add(role);
        } else if (containsAny(stack, "java", "spring", "mysql", "redis")) {
            result.getRecommendedRoles().add("Java 后端开发");
        } else if (containsAny(stack, "vue", "react", "typescript", "javascript")) {
            result.getRecommendedRoles().add("前端开发");
        } else if (containsAny(stack, "python", "django", "fastapi")) {
            result.getRecommendedRoles().add("Python 后端开发");
        } else {
            result.getRecommendedRoles().add("软件开发工程师");
        }
    }

    private ResumeScoreResult.ResumeInterviewDefaults buildDefaults(
            ResumeScoreResult result, ResumeDTO resume, String role, Set<String> stack) {
        ResumeScoreResult.ResumeInterviewDefaults defaults = new ResumeScoreResult.ResumeInterviewDefaults();
        defaults.setTargetRole(StringUtils.hasText(role)
                ? role
                : result.getRecommendedRoles().stream().findFirst().orElse("软件开发工程师"));
        defaults.setTargetLevel(inferLevel(resume));
        defaults.setDifficulty(result.getTotalScore() >= 80 ? "HARD" : result.getTotalScore() >= 55 ? "MEDIUM" : "EASY");
        defaults.setFocusTopics(result.getRecommendedTopics().stream().distinct().limit(10).collect(Collectors.toList()));
        List<String> styles = new ArrayList<>();
        styles.add("TECHNICAL");
        styles.add("PROJECT_DEEP_DIVE");
        if (!resume.getInternshipList().isEmpty()) styles.add("BEHAVIORAL");
        defaults.setInterviewStyles(styles);
        return defaults;
    }

    private int educationScore(List<Education> list) {
        if (list == null || list.isEmpty()) return 25;
        Education education = list.get(0);
        int score = switch (normalize(education == null ? null : education.getDegree())) {
            case "博士" -> 92;
            case "硕士" -> 88;
            case "本科" -> 78;
            case "大专" -> 62;
            default -> 48;
        };
        if (has(education == null ? null : education.getSchool())) score += 4;
        if (has(education == null ? null : education.getMajor())) score += 4;
        if (has(education == null ? null : education.getStartTime()) && has(education == null ? null : education.getEndTime())) score += 4;
        if (has(education == null ? null : education.getGpa())) score += 3;
        return clamp(score);
    }

    private int internshipScore(List<Internship> list) {
        if (list == null || list.isEmpty()) return 20;
        int score = Math.min(55, list.size() * 25);
        for (Internship item : list) {
            if (item == null) continue;
            if (has(item.getCompany())) score += 5;
            if (has(item.getPosition())) score += 7;
            if (has(item.getDescription())) score += Math.min(8, item.getDescription().trim().length() / 50 + 2);
            if (has(item.getStartTime()) && has(item.getEndTime())) score += 3;
        }
        return clamp(score);
    }

    private int projectScore(List<Project> list) {
        if (list == null || list.isEmpty()) return 20;
        int score = Math.min(48, list.size() * 18);
        for (Project item : list) {
            if (item == null) continue;
            if (has(item.getProjectName())) score += 5;
            if (has(item.getRole())) score += 7;
            if (has(item.getDescription())) score += Math.min(15, item.getDescription().trim().length() / 45 + 3);
            if (item.getTechStack() != null && !item.getTechStack().isEmpty()) score += 8;
            if (has(item.getStartTime()) && has(item.getEndTime())) score += 3;
        }
        return clamp(score);
    }

    private int skillsScore(List<String> skills) {
        if (skills == null || skills.isEmpty()) return 15;
        long usable = skills.stream().filter(this::has).count();
        return clamp((int) Math.min(100, usable * 12 + (usable >= 4 ? 18 : 0)));
    }

    private int techStackScore(Set<String> stack) {
        if (stack.isEmpty()) return 15;
        return clamp(20 + stack.size() * 10);
    }

    private int roleMatchScore(ResumeDTO resume, String role, Set<String> stack) {
        if (!has(role)) return clamp(35 + Math.min(45, stack.size() * 8));
        String normalizedRole = normalize(role);
        String text = normalize(joinResumeText(resume));
        int score = text.contains(normalizedRole) ? 55 : 30;
        for (String token : splitTokens(role)) {
            if (text.contains(normalize(token))) score += 8;
        }
        if (normalizedRole.contains("后端") && containsAny(stack, "java", "spring", "mysql", "redis", "python")) score += 20;
        if (normalizedRole.contains("前端") && containsAny(stack, "vue", "react", "typescript", "javascript")) score += 20;
        if (normalizedRole.contains("全栈") && stack.size() >= 5) score += 15;
        return clamp(score);
    }

    private int completenessScore(ResumeDTO resume) {
        int filled = 0;
        int total = 0;
        if (resume.getBasicInfo() != null) {
            String[] values = {resume.getBasicInfo().getName(), resume.getBasicInfo().getEmail(), resume.getBasicInfo().getCity()};
            for (String value : values) { total++; if (has(value)) filled++; }
        }
        for (Education item : safeList(resume.getEducationList())) {
            total += 4;
            if (item != null) {
                if (has(item.getSchool())) filled++;
                if (has(item.getMajor())) filled++;
                if (has(item.getDegree())) filled++;
                if (has(item.getEndTime())) filled++;
            }
        }
        for (Project item : safeList(resume.getProjectList())) {
            total += 3;
            if (item != null) {
                if (has(item.getProjectName())) filled++;
                if (has(item.getDescription())) filled++;
                if (item.getTechStack() != null && !item.getTechStack().isEmpty()) filled++;
            }
        }
        for (Internship item : safeList(resume.getInternshipList())) {
            total += 3;
            if (item != null) {
                if (has(item.getCompany())) filled++;
                if (has(item.getPosition())) filled++;
                if (has(item.getDescription())) filled++;
            }
        }
        return total == 0 ? 0 : clamp(Math.round(filled * 100f / total));
    }

    private String educationExplanation(List<Education> list) {
        return list == null || list.isEmpty() ? "未识别到教育经历" : "按最高学历、学校、专业、时间和 GPA 字段完整度计算";
    }

    private String internshipExplanation(List<Internship> list) {
        return list == null || list.isEmpty() ? "未识别到实习经历" : "按经历数量、公司、职位、时间和工作内容完整度计算";
    }

    private String projectExplanation(List<Project> list) {
        return list == null || list.isEmpty() ? "未识别到项目经历" : "按项目数量、个人角色、技术栈、描述和成果字段计算";
    }

    private Set<String> collectTechStack(ResumeDTO resume) {
        Set<String> values = new LinkedHashSet<>();
        if (resume.getSkills() != null) resume.getSkills().stream().filter(this::has).map(String::trim).forEach(values::add);
        for (Project project : safeList(resume.getProjectList())) {
            if (project != null && project.getTechStack() != null) {
                project.getTechStack().stream().filter(this::has).map(String::trim).forEach(values::add);
            }
        }
        return values;
    }

    private String joinResumeText(ResumeDTO resume) {
        List<String> values = new ArrayList<>();
        if (resume.getTargetRole() != null) values.add(resume.getTargetRole());
        if (resume.getSkills() != null) values.addAll(resume.getSkills());
        for (Project project : safeList(resume.getProjectList())) {
            if (project == null) continue;
            values.add(project.getRole()); values.add(project.getDescription()); values.add(String.join(" ", safeList(project.getTechStack())));
        }
        for (Internship internship : safeList(resume.getInternshipList())) {
            if (internship == null) continue;
            values.add(internship.getPosition()); values.add(internship.getDescription());
        }
        return values.stream().filter(this::has).collect(Collectors.joining(" "));
    }

    private String inferLevel(ResumeDTO resume) {
        int experience = safeList(resume.getInternshipList()).size() + safeList(resume.getProjectList()).size();
        return experience >= 5 ? "SENIOR" : experience >= 2 ? "MID" : "JUNIOR";
    }

    private boolean containsAny(Set<String> values, String... candidates) {
        for (String value : values) {
            String normalized = normalize(value);
            for (String candidate : candidates) if (normalized.contains(normalize(candidate))) return true;
        }
        return false;
    }

    private List<String> splitTokens(String value) {
        if (!has(value)) return Collections.emptyList();
        return Arrays.stream(value.split("[\\s/、,，_-]+"))
                .filter(this::has)
                .collect(Collectors.toList());
    }

    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private boolean has(String value) { return StringUtils.hasText(value); }
    private int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    private int clamp(long value) { return clamp((int) value); }
    private <T> List<T> safeList(List<T> list) { return list == null ? Collections.emptyList() : list; }
}
