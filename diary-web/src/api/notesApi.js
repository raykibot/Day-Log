import {http} from './http'

function toCreateFormData(note) {
  const body = new FormData()
  const noteDto = {
    title: note.title.trim() || null,
    mdName: note.mdName,
    content: note.content,
    category: note.category,
  }
  body.append('noteDto', new Blob([JSON.stringify(noteDto)], {type: 'application/json'}))
  for (const image of note.images ?? []) body.append('imageFile', image.file ?? image)
  return body
}

function toFormData(note) {
  const body = new FormData()
  body.append('category', note.category)
  if (note.title?.trim()) body.append('title', note.title.trim())
  if (note.document) body.append('document', note.document)
  for (const id of note.keepImageIds ?? []) body.append('keepImageIds', String(id))
  for (const image of note.images ?? []) body.append('images', image.file ?? image)
  return body
}

export const notesApi = {
  list: (params = {}) => http.get('/notes/noteList', {params}),
  get: (id) => http.get(`/notes/${id}`),
  create: (note) => http.post('/notes/create', toCreateFormData(note), {timeout: 120000}),
  update: (id, note) => http.put(`/notes/${id}`, toFormData(note)),
  remove: (id) => http.delete(`/notes/${id}`),
}
