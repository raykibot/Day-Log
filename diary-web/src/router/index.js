import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/notes' },
    { path: '/notes', name: 'notes', component: () => import('@/views/NotesListView.vue') },
    { path: '/notes/new', name: 'new-note', component: () => import('@/views/NoteFormView.vue') },
    { path: '/notes/:id', name: 'note-detail', component: () => import('@/views/NoteDetailView.vue') },
    { path: '/notes/:id/edit', name: 'edit-note', component: () => import('@/views/NoteFormView.vue') },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
