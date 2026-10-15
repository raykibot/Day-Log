import { notesApi } from '@/api/notesApi'

// New records are saved to the backend. Edit and delete still use this browser session.
const drafts = new Map()
const removed = new Set()
let nextImageId = -1

function revokeImages(images) {
  for (const image of images) {
    if (image.url?.startsWith('blob:')) URL.revokeObjectURL(image.url)
  }
}

async function readAll(category) {
  const notes = await notesApi.list(category ? { category } : {})
  if (!Array.isArray(notes)) throw new Error('后端返回的记录列表格式不正确。')
  return notes
}

export const notesRepository = {
  async list({ category } = {}) {
    const remote = await readAll(category)
    const byId = new Map(remote.map((note) => [note.id, drafts.get(note.id) ?? note]))
    for (const note of drafts.values()) byId.set(note.id, note)
    return [...byId.values()]
      .filter((note) => !removed.has(note.id) && (!category || note.category === category))
      .sort((a, b) => b.createTime.localeCompare(a.createTime) || b.id - a.id)
  },
  async get(id) {
    const noteId = Number(id)
    if (!Number.isSafeInteger(noteId) || noteId === 0 || removed.has(noteId)) return null
    if (drafts.has(noteId)) return drafts.get(noteId)
    if (noteId < 0) return null
    try {
      return await notesApi.get(noteId)
    } catch (error) {
      if (error.response?.status === 404) return null
      throw error
    }
  },
  async create(input) {
    const created = await notesApi.create(input)
    if (created !== true) throw new Error('后端未确认保存，请检查记录列表。')
    return true
  },
  async update(id, input) {
    const note = await this.get(id)
    if (!note) return null
    const kept = note.images.filter((image) => input.keepImageIds.includes(image.id))
    revokeImages(note.images.filter((image) => !input.keepImageIds.includes(image.id)))
    const updated = {
      ...note,
      title: input.title.trim() || null,
      mdName: input.mdName,
      content: input.content,
      category: input.category,
      updateTime: new Date().toISOString(),
      images: [...kept, ...input.images.map((image) => ({ ...image, id: nextImageId-- }))],
    }
    drafts.set(note.id, updated)
    return updated
  },
  async remove(id) {
    const note = await this.get(id)
    if (!note) return false
    revokeImages(note.images)
    drafts.delete(note.id)
    removed.add(note.id)
    return true
  },
}
