<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Check, FileText, ImagePlus, Leaf, Paperclip, Save, UploadCloud, X } from '@lucide/vue'
import { apiErrorMessage } from '@/api/http'
import { notesRepository } from '@/data/notesRepository'
import { displayTitle } from '@/lib/notes'
import { renderMarkdown } from '@/lib/markdown'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => route.name === 'edit-note')
const existing = ref(null)
const notFound = ref(false)
const loadError = ref('')
const form = reactive({ title: '', category: 'LIFE', mdName: '', content: '' })
const documentFile = ref(null)
const pendingImages = ref([])
const keepImageIds = ref([])
const documentInput = ref(null)
const imageInput = ref(null)
const error = ref('')
const saving = ref(false)
const dragOver = ref(false)

const keptImages = computed(() => existing.value?.images.filter((image) => keepImageIds.value.includes(image.id)) ?? [])
const allImages = computed(() => [...keptImages.value, ...pendingImages.value])
const previewHtml = computed(() => renderMarkdown(form.content, allImages.value))
const backTo = computed(() => isEdit.value && existing.value ? `/notes/${existing.value.id}` : '/notes')
const validFileName = (name) => name && name.length <= 255 && name !== '.' && name !== '..' && !/[\\/<>:"|?*\x00-\x1F]/.test(name)

watch(() => route.fullPath, async () => {
  for (const image of pendingImages.value) URL.revokeObjectURL(image.url)
  pendingImages.value = []
  error.value = ''
  notFound.value = false
  loadError.value = ''
  if (!isEdit.value) {
    existing.value = null
    Object.assign(form, { title: '', category: 'LIFE', mdName: '', content: '' })
    documentFile.value = null
    keepImageIds.value = []
    return
  }
  let note
  try {
    note = await notesRepository.get(route.params.id)
  } catch (cause) {
    loadError.value = apiErrorMessage(cause)
    return
  }
  if (!note) {
    notFound.value = true
    return
  }
  existing.value = note
  Object.assign(form, { title: note.title ?? '', category: note.category, mdName: note.mdName, content: note.content })
  documentFile.value = null
  keepImageIds.value = note.images.map((image) => image.id)
}, { immediate: true })

async function chooseDocument(file) {
  error.value = ''
  if (!file) return
  if (!validFileName(file.name)) {
    error.value = 'Markdown 文件名无效。'
    return
  }
  if (!/\.md$/i.test(file.name)) {
    error.value = '请选择 .md 格式的 Markdown 文件。'
    return
  }
  if (file.size === 0 || file.size > 1024 * 1024) {
    error.value = 'Markdown 文件需大于 0 字节，且不超过 1 MB。'
    return
  }
  try {
    const content = new TextDecoder('utf-8', { fatal: true }).decode(await file.arrayBuffer())
    form.mdName = file.name
    form.content = content.replace(/^\uFEFF/, '')
    documentFile.value = file
  } catch {
    error.value = 'Markdown 文件必须使用 UTF-8 编码。'
  }
}

function onDocumentChange(event) {
  chooseDocument(event.target.files?.[0])
  event.target.value = ''
}

function onDocumentDrop(event) {
  dragOver.value = false
  chooseDocument(event.dataTransfer.files?.[0])
}

function addImages(event) {
  error.value = ''
  const files = [...(event.target.files ?? [])]
  event.target.value = ''
  const names = new Set(allImages.value.map((image) => image.originalName.toLowerCase()))
  for (const file of files) {
    if (!validFileName(file.name)) {
      error.value = '图片文件名无效。'
      return
    }
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      error.value = '图片仅支持 JPEG、PNG 或 WebP。'
      return
    }
    if (file.size === 0 || file.size > 20 * 1024 * 1024) {
      error.value = '每张图片需大于 0 字节，且不超过 20 MB。'
      return
    }
    if (names.has(file.name.toLowerCase())) {
      error.value = `图片名称重复：${file.name}`
      return
    }
    names.add(file.name.toLowerCase())
  }
  pendingImages.value.push(...files.map((file) => ({
    file,
    originalName: file.name,
    mimeType: file.type,
    sizeBytes: file.size,
    url: URL.createObjectURL(file),
  })))
}

function removePending(index) {
  URL.revokeObjectURL(pendingImages.value[index].url)
  pendingImages.value.splice(index, 1)
}

function removeExisting(id) {
  keepImageIds.value = keepImageIds.value.filter((item) => item !== id)
}

function validateReferences() {
  const names = new Set(allImages.value.map((image) => image.originalName.toLowerCase()))
  for (const match of form.content.matchAll(/!\[[^\]]*]\(([^)]+)\)/g)) {
    const name = match[1].trim()
    if (!name || name.includes('/') || name.includes('\\') || !names.has(name.toLowerCase())) {
      return `正文引用了未附加的图片：${name || '空文件名'}`
    }
  }
  return ''
}

async function submit() {
  error.value = ''
  if (form.title.trim().length > 200) {
    error.value = '标题不能超过 200 字。'
    return
  }
  if (!form.mdName || (!isEdit.value && !documentFile.value)) {
    error.value = '请先上传一份 .md 文件。'
    return
  }
  if (!['WORK', 'LIFE'].includes(form.category)) {
    error.value = '请选择工作或生活分类。'
    return
  }
  error.value = validateReferences()
  if (error.value) return

  saving.value = true
  try {
    const input = {
      title: form.title,
      category: form.category,
      mdName: form.mdName,
      content: form.content,
      document: documentFile.value,
      images: pendingImages.value,
      keepImageIds: keepImageIds.value,
    }
    const saved = isEdit.value
      ? await notesRepository.update(existing.value.id, input)
      : await notesRepository.create(input)
    if (isEdit.value) {
      pendingImages.value = [] // 编辑仍保存在当前页面会话中，图片预览交给内存记录使用。
      await router.push(`/notes/${saved.id}`)
    } else {
      for (const image of pendingImages.value) URL.revokeObjectURL(image.url)
      pendingImages.value = []
      await router.push('/notes')
    }
  } catch (cause) {
    error.value = cause.isAxiosError ? apiErrorMessage(cause) : cause.message || '保存失败，请重试。'
  } finally {
    saving.value = false
  }
}

onBeforeUnmount(() => {
  for (const image of pendingImages.value) URL.revokeObjectURL(image.url)
})
</script>

<template>
  <main class="mx-auto max-w-[1050px] px-5 pb-8 pt-9 sm:px-8 sm:pt-12">
    <RouterLink :to="backTo" class="inline-flex items-center gap-2 text-[13px] font-medium text-[#899a8a] transition hover:text-[#45614c]"><ArrowLeft :size="16" /> {{ isEdit ? '返回记录详情' : '返回所有记录' }}</RouterLink>

    <div v-if="loadError" role="alert" class="mt-8 rounded-[25px] bg-white px-6 py-20 text-center text-sm text-[#ab6658]">{{ loadError }}</div>
    <div v-else-if="notFound" class="mt-8 rounded-[25px] bg-white px-6 py-20 text-center"><h1 class="text-2xl font-semibold text-[#405445]">没有找到要编辑的记录</h1><RouterLink to="/notes" class="mt-4 inline-block text-sm text-[#5d795f]">返回列表</RouterLink></div>

    <template v-else>
      <div class="mb-8 mt-8"><p class="text-[11px] font-semibold uppercase tracking-[0.23em] text-[#92a793]">{{ isEdit ? 'EDIT YOUR STORY' : 'A NEW PAGE' }}</p><h1 class="mt-2 text-[30px] font-semibold tracking-[-0.04em] text-[#354a3a] sm:text-[38px]">{{ isEdit ? '编辑这篇记录' : '写下今天的故事' }}</h1><p class="mt-3 text-sm text-[#8e9d90]">{{ isEdit ? '可以保留原 Markdown 文件，更新标题、分类或图片。' : '上传一份 Markdown，给这一天留下自己的注脚。' }}</p><p class="mt-2 text-xs text-[#b58773]">{{ isEdit ? '编辑暂时只保存在本次页面会话。' : '保存后会写入数据库，并返回记录列表。' }}</p></div>

      <div class="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_330px]">
        <form class="paper-shadow space-y-8 rounded-[24px] border border-[#e8ebe4] bg-white p-6 sm:p-8" @submit.prevent="submit">
          <div>
            <label for="note-title" class="mb-2 block text-[13px] font-semibold text-[#4a5d4d]">记录标题 <span class="font-normal text-[#a2ada2]">（可选）</span></label>
            <input id="note-title" v-model="form.title" maxlength="201" type="text" placeholder="留空时使用 Markdown 文件名" class="w-full rounded-xl border border-[#e3e9e0] bg-[#fcfdfa] px-4 py-3 text-[14px] outline-none transition placeholder:text-[#b4beb1] focus:border-[#91a990] focus:ring-2 focus:ring-[#dfe9dc]" />
          </div>

          <fieldset>
            <legend class="mb-3 text-[13px] font-semibold text-[#4a5d4d]">记录分类</legend>
            <div class="grid grid-cols-2 gap-3">
              <label v-for="category in [{ key: 'WORK', title: '工作', hint: '留住每一步进展' }, { key: 'LIFE', title: '生活', hint: '收藏日常的温度' }]" :key="category.key" class="flex cursor-pointer items-center gap-3 rounded-xl border px-4 py-3 transition" :class="form.category === category.key ? 'border-[#9aaf98] bg-[#f2f7ef]' : 'border-[#e7ebe3] hover:border-[#cbd8ca]'">
                <input v-model="form.category" type="radio" name="category" :value="category.key" class="accent-[#4d6b52]" />
                <span><span class="block text-[13px] font-semibold text-[#506451]">{{ category.title }}</span><span class="mt-0.5 block text-[11px] text-[#a3afa2]">{{ category.hint }}</span></span>
              </label>
            </div>
          </fieldset>

          <div>
            <div class="mb-2 flex items-center justify-between"><label for="document-file" class="text-[13px] font-semibold text-[#4a5d4d]">Markdown 文件 <span v-if="!isEdit" class="text-[#bf8a76]">*</span></label><span class="text-[11px] text-[#a6b0a4]">.md · UTF-8 · 不超过 1 MB</span></div>
            <input id="document-file" ref="documentInput" type="file" accept=".md,text/markdown" class="sr-only" @change="onDocumentChange" />
            <div role="button" tabindex="0" class="flex min-h-[136px] cursor-pointer flex-col items-center justify-center rounded-[16px] border-2 border-dashed px-4 py-5 text-center transition" :class="dragOver ? 'border-[#86a184] bg-[#f0f6ed]' : 'border-[#dce6d9] bg-[#fafcf8] hover:border-[#a9bea6]'" @click="documentInput?.click()" @keydown.enter.prevent="documentInput?.click()" @keydown.space.prevent="documentInput?.click()" @dragover.prevent="dragOver = true" @dragleave.prevent="dragOver = false" @drop.prevent="onDocumentDrop">
              <UploadCloud :size="25" class="text-[#85a186]" :stroke-width="1.7" />
              <span class="mt-2 text-[13px] font-semibold text-[#637d66]">{{ form.mdName ? '更换 Markdown 文件' : '点击选择或拖入 .md 文件' }}</span>
              <span class="mt-1 text-[11px] text-[#a0aea0]">{{ form.mdName || '你的内容会在右侧实时预览' }}</span>
            </div>
            <p v-if="isEdit && !documentFile" class="mt-2 text-[11px] text-[#a2afa2]">不选择新文件时，保留原正文和文件名。</p>
          </div>

          <div>
            <div class="mb-3 flex items-center justify-between"><label for="image-files" class="text-[13px] font-semibold text-[#4a5d4d]">图片附件 <span class="font-normal text-[#a2ada2]">（可选）</span></label><span class="text-[11px] text-[#a6b0a4]">JPEG / PNG / WebP · 每张 ≤ 20 MB</span></div>
            <input id="image-files" ref="imageInput" type="file" accept="image/jpeg,image/png,image/webp" multiple class="sr-only" @change="addImages" />
            <button type="button" class="inline-flex items-center gap-2 rounded-full border border-[#dce7dc] px-4 py-2.5 text-[12px] font-semibold text-[#638067] transition hover:bg-[#f2f7ef]" @click="imageInput?.click()"><ImagePlus :size="16" /> 添加图片</button>
            <p class="mt-2 text-[11px] leading-5 text-[#a2afa2]">若正文使用 <code>![说明](图片名.png)</code>，请附加同名图片。</p>
            <div v-if="allImages.length" class="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
              <div v-for="image in keptImages" :key="`old-${image.id}`" class="relative overflow-hidden rounded-xl border border-[#e5e9e2] bg-[#f7f9f5]"><img :src="image.url" :alt="image.originalName" class="aspect-[4/3] w-full object-cover" /><button type="button" class="absolute right-1.5 top-1.5 rounded-full bg-white/95 p-1 text-[#718475]" :aria-label="`移除 ${image.originalName}`" @click="removeExisting(image.id)"><X :size="14" /></button><p class="truncate px-2 py-1.5 text-[10px] text-[#879588]">{{ image.originalName }}</p></div>
              <div v-for="(image, index) in pendingImages" :key="image.url" class="relative overflow-hidden rounded-xl border border-[#e5e9e2] bg-[#f7f9f5]"><img :src="image.url" :alt="image.originalName" class="aspect-[4/3] w-full object-cover" /><button type="button" class="absolute right-1.5 top-1.5 rounded-full bg-white/95 p-1 text-[#718475]" :aria-label="`移除 ${image.originalName}`" @click="removePending(index)"><X :size="14" /></button><p class="truncate px-2 py-1.5 text-[10px] text-[#879588]">{{ image.originalName }}</p></div>
            </div>
          </div>

          <p v-if="error" role="alert" class="rounded-xl border border-[#f0d4cb] bg-[#fff7f3] px-4 py-3 text-[12px] leading-5 text-[#ab6658]">{{ error }}</p>
          <div class="flex flex-wrap items-center justify-end gap-3 border-t border-[#eef0ec] pt-6"><RouterLink :to="backTo" class="rounded-full px-5 py-2.5 text-[13px] font-medium text-[#879889]">取消</RouterLink><button type="submit" :disabled="saving" class="inline-flex items-center gap-2 rounded-full bg-[#455e4b] px-6 py-2.5 text-[13px] font-semibold text-white transition hover:bg-[#354d3c] disabled:opacity-60"><Save :size="15" />{{ saving ? '保存中…' : (isEdit ? '保存修改' : '保存记录') }}</button></div>
        </form>

        <aside class="soft-shadow overflow-hidden rounded-[24px] border border-[#e7ebe3] bg-white lg:sticky lg:top-[100px]">
          <div class="flex items-center justify-between border-b border-[#eef1eb] bg-[#f8faf6] px-5 py-4"><div class="flex items-center gap-2 text-[13px] font-semibold text-[#59715d]"><FileText :size="16" /> 内容预览</div><span class="rounded-full bg-white px-2.5 py-1 text-[10px] text-[#99a89a]">Markdown</span></div>
          <div class="max-h-[530px] min-h-[260px] overflow-y-auto p-5">
            <template v-if="form.mdName"><h2 class="mb-2 text-[18px] font-semibold text-[#3c5040]">{{ form.title.trim() || displayTitle(form) }}</h2><p class="mb-5 flex items-center gap-1.5 text-[11px] text-[#a2aea2]"><Paperclip :size="12" /> {{ form.mdName }}</p><div class="markdown-body text-[13px]" v-html="previewHtml"></div></template>
            <div v-else class="flex min-h-[210px] flex-col items-center justify-center text-center"><span class="mb-3 flex size-12 items-center justify-center rounded-2xl bg-[#f1f5ef] text-[#9ab09a]"><Leaf :size="23" /></span><p class="text-[13px] font-medium text-[#879b89]">上传文件后，在这里预览</p><p class="mt-1 text-[11px] text-[#acb9aa]">让内容先和你见个面</p></div>
          </div>
          <div v-if="form.mdName" class="flex items-center gap-1.5 border-t border-[#eef1eb] px-5 py-3 text-[11px] text-[#91a092]"><Check :size="13" /> 仅供预览，尚未保存到后端</div>
        </aside>
      </div>
    </template>
  </main>
</template>
