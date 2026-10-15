export const CATEGORY_LABELS = { WORK: '工作', LIFE: '生活' }

export function displayTitle(note) {
  return note.title || note.mdName?.replace(/\.md$/i, '') || '未命名记录'
}

export function formatDate(value, withTime = false) {
  if (!value) return ''
  const date = new Date(/(?:Z|[+-]\d\d:\d\d)$/.test(value) ? value : `${value}+08:00`)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric',
    timeZone: 'Asia/Shanghai',
    ...(withTime ? { hour: '2-digit', minute: '2-digit', hour12: false } : {}),
  }).format(date)
}

export function excerpt(markdown, maxLength = 92) {
  const plain = markdown
    .replace(/!\[[^\]]*]\([^)]*\)/g, '')
    .replace(/\[([^\]]+)]\([^)]*\)/g, '$1')
    .replace(/[#>*_`~\-]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return plain.length > maxLength ? `${plain.slice(0, maxLength)}…` : plain
}
