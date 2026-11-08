/** Convert backend, axios and network failures into consistent user-facing text. */
export function formatApiError(error: unknown, fallback: string): string {
  const message = error instanceof Error ? error.message : String(error || '')
  if (/不存在|not found|404/i.test(message)) return '会话不存在，可能已被删除，请从历史面试重新选择。'
  if (/结束|finished|已结束/i.test(message)) return '面试已结束，不能继续提交回答。'
  // SQL/schema failures must be classified before the generic model wording:
  // a create-session schema mismatch is not an LLM outage and needs a migration fix.
  if (/SQLSyntaxError|Unknown column|bad SQL grammar|数据库|database|SQL/i.test(message)) return `数据库结构错误：${message}`
  if (/权限|unauthorized|forbidden|401|403/i.test(message)) return `权限或配置错误：${message}`
  if (/LLM|模型|百炼|AI|调用模型/i.test(message)) return `LLM 调用失败：${message}`
  if (/网络|network|timeout|超时|ECONN/i.test(message)) return '网络失败，请检查后端服务和网络连接后重试。'
  return `${fallback}${message ? `：${message}` : ''}`
}

