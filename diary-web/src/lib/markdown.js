import { marked } from 'marked'
import DOMPurify from 'dompurify'

export function renderMarkdown(content, images = []) {
  const safeHtml = DOMPurify.sanitize(marked.parse(content || ''), { USE_PROFILES: { html: true } })
  const document = new DOMParser().parseFromString(safeHtml, 'text/html')

  // The backend only accepts image references by same-directory filename.
  for (const image of document.querySelectorAll('img')) {
    const source = image.getAttribute('src') || ''
    let name
    try {
      name = decodeURIComponent(source)
    } catch {
      name = source
    }
    const attachment = images.find((item) => item.originalName === name)
    if (attachment) image.setAttribute('src', attachment.url)
    else image.replaceWith(document.createTextNode(`[缺少图片：${name}]`))
  }
  for (const link of document.querySelectorAll('a')) {
    link.setAttribute('rel', 'noopener noreferrer')
    link.setAttribute('target', '_blank')
  }
  return document.body.innerHTML
}
