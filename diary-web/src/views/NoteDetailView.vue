<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CalendarDays, ChevronLeft, ChevronRight, FileText, Image, Leaf, Minus, Pencil, Plus, Trash2, X } from '@lucide/vue'
import { apiErrorMessage } from '@/api/http'
import { notesRepository } from '@/data/notesRepository'
import { CATEGORY_LABELS, displayTitle, formatDate } from '@/lib/notes'
import { renderMarkdown } from '@/lib/markdown'

const route = useRoute()
const router = useRouter()
const note = ref(null)
const confirmDelete = ref(false)
const loading = ref(true)
const loadError = ref('')
const viewerIndex = ref(null)
const viewerZoom = ref(1)
const viewerDialog = ref(null)
let loadVersion = 0
let previousOverflow = ''
let viewerTrigger = null
const html = computed(() => note.value ? renderMarkdown(note.value.content, note.value.images) : '')
const viewerImage = computed(() => viewerIndex.value === null ? null : note.value?.images[viewerIndex.value])

async function openViewer(index, event) {
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  viewerTrigger = event.currentTarget
  viewerIndex.value = index
  viewerZoom.value = 1
  await nextTick()
  viewerDialog.value?.focus()
}

function closeViewer() {
  if (viewerIndex.value === null) return
  viewerIndex.value = null
  viewerZoom.value = 1
  document.body.style.overflow = previousOverflow
  nextTick(() => viewerTrigger?.focus())
}

function showImage(offset) {
  const next = viewerIndex.value + offset
  if (next < 0 || next >= note.value.images.length) return
  viewerIndex.value = next
  viewerZoom.value = 1
}

function zoomImage(amount) {
  viewerZoom.value = Math.min(3, Math.max(1, viewerZoom.value + amount))
}

function onViewerKeydown(event) {
  if (viewerIndex.value === null) return
  if (event.key === 'Escape') closeViewer()
  else if (event.key === 'ArrowLeft') showImage(-1)
  else if (event.key === 'ArrowRight') showImage(1)
  else if (event.key === '+' || event.key === '=') zoomImage(0.25)
  else if (event.key === '-') zoomImage(-0.25)
  else return
  event.preventDefault()
}

onMounted(() => window.addEventListener('keydown', onViewerKeydown))
onUnmounted(() => {
  window.removeEventListener('keydown', onViewerKeydown)
  if (viewerIndex.value !== null) document.body.style.overflow = previousOverflow
})

async function load(id) {
  const version = ++loadVersion
  closeViewer()
  note.value = null
  loading.value = true
  loadError.value = ''
  confirmDelete.value = false
  try {
    const result = await notesRepository.get(id)
    if (version === loadVersion) note.value = result
  } catch (error) {
    if (version === loadVersion) loadError.value = apiErrorMessage(error)
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })

async function remove() {
  if (!note.value) return
  await notesRepository.remove(note.value.id)
  await router.push('/notes')
}
</script>

<template>
  <main class="mx-auto max-w-[1000px] px-5 pb-8 pt-9 sm:px-8 sm:pt-12">
    <RouterLink to="/notes" class="inline-flex items-center gap-2 text-[13px] font-medium text-[#899a8a] transition hover:text-[#45614c]"><ArrowLeft :size="16" /> 返回所有记录</RouterLink>

    <div v-if="loading" role="status" class="mt-8 rounded-[26px] border border-[#e7ebe3] bg-white px-6 py-20 text-center text-sm text-[#879789]">正在读取记录…</div>
    <div v-else-if="loadError" role="alert" class="mt-8 rounded-[26px] border border-[#f0d4cb] bg-[#fff7f3] px-6 py-14 text-center text-sm text-[#ab6658]">
      <p>{{ loadError }}</p><button type="button" class="mt-4 font-semibold underline" @click="load(route.params.id)">重试</button>
    </div>
    <template v-else-if="note">
      <article class="paper-shadow mt-8 overflow-hidden rounded-[28px] border border-[#e8ebe3] bg-white sm:mt-9">
        <div class="border-b border-[#eef0eb] px-6 pb-9 pt-9 sm:px-12 sm:pb-11 sm:pt-11 lg:px-16">
          <span class="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-[11px] font-semibold" :class="note.category === 'WORK' ? 'bg-[#eaf1e9] text-[#69846c]' : 'bg-[#f9eee4] text-[#b78c6a]'">
            <FileText v-if="note.category === 'WORK'" :size="12" /><Leaf v-else :size="12" /> {{ CATEGORY_LABELS[note.category] }}记录
          </span>
          <h1 class="mt-5 text-[29px] font-semibold leading-[1.35] tracking-[-0.035em] text-[#34473a] sm:text-[40px]">{{ displayTitle(note) }}</h1>
          <div class="mt-5 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-[#a1ada2]">
            <span class="inline-flex items-center gap-1.5"><CalendarDays :size="14" /> 记录于 {{ formatDate(note.createTime, true) }}</span>
            <span v-if="note.updateTime">更新于 {{ formatDate(note.updateTime, true) }}</span>
            <span class="inline-flex items-center gap-1.5"><FileText :size="14" /> {{ note.mdName }}</span>
          </div>
        </div>

        <div class="px-6 py-9 sm:px-12 sm:py-11 lg:px-16">
          <div v-if="note.content" class="markdown-body" v-html="html"></div>
          <p v-else class="text-sm text-[#9cab9d]">这篇记录还没有正文。</p>

          <section v-if="note.images.length" class="mt-12 border-t border-[#edf0ea] pt-8" aria-label="记录图片">
            <h2 class="mb-5 flex items-center gap-2 text-[15px] font-semibold text-[#435847]"><Image :size="17" /> 图片附件 <span class="text-xs font-normal text-[#a0aaa0]">{{ note.images.length }} 张</span></h2>
            <div class="grid grid-cols-2 gap-3 sm:grid-cols-3">
              <button v-for="(image, index) in note.images" :key="image.id" type="button" :aria-label="`查看图片 ${index + 1}：${image.originalName}`" class="group overflow-hidden rounded-xl border border-[#edf0eb] bg-[#f4f6f1] text-left" @click="openViewer(index, $event)">
                <img :src="image.url" :alt="image.originalName" class="aspect-[4/3] w-full object-cover transition-transform group-hover:scale-105" />
                <p class="truncate px-3 py-2 text-[11px] text-[#879789]">{{ image.originalName }}</p>
              </button>
            </div>
          </section>
        </div>
      </article>

      <div class="mt-6 flex flex-wrap items-center justify-between gap-4 px-1">
        <p class="text-xs text-[#a4aea3]">每一次记录，都是和今天的自己相遇。</p>
        <div class="flex items-center gap-2">
          <button type="button" class="inline-flex items-center gap-2 rounded-full px-4 py-2.5 text-[13px] font-medium text-[#b88175] transition hover:bg-[#f8eeeb]" @click="confirmDelete = true"><Trash2 :size="15" /> 删除</button>
          <RouterLink :to="`/notes/${note.id}/edit`" class="inline-flex items-center gap-2 rounded-full bg-[#455e4b] px-5 py-2.5 text-[13px] font-semibold text-white transition hover:bg-[#354d3c]"><Pencil :size="15" /> 编辑记录</RouterLink>
        </div>
      </div>
    </template>

    <div v-else class="mt-8 rounded-[26px] border border-[#e7ebe3] bg-white px-6 py-20 text-center">
      <h1 class="text-2xl font-semibold text-[#405445]">没有找到这篇记录</h1>
      <p class="mt-3 text-sm text-[#92a092]">它可能已被删除，或链接中的编号有误。</p>
      <RouterLink to="/notes" class="mt-6 inline-flex items-center gap-2 text-sm font-semibold text-[#5d795f]"><ArrowLeft :size="15" /> 返回记录列表</RouterLink>
    </div>

    <div v-if="confirmDelete" class="fixed inset-0 z-50 flex items-center justify-center bg-[#283a2e]/35 px-5 backdrop-blur-[3px]" role="presentation" @click.self="confirmDelete = false">
      <div role="dialog" aria-modal="true" aria-labelledby="delete-title" class="w-full max-w-[390px] rounded-[24px] bg-white p-7 shadow-xl">
        <div class="flex items-start justify-between"><span class="flex size-11 items-center justify-center rounded-xl bg-[#faf0eb] text-[#be8675]"><Trash2 :size="20" /></span><button type="button" class="rounded-lg p-1 text-[#9ba69b] hover:bg-[#f2f4ef]" aria-label="关闭删除确认" @click="confirmDelete = false"><X :size="20" /></button></div>
        <h2 id="delete-title" class="mt-5 text-xl font-semibold text-[#354638]">删除这篇记录？</h2>
        <p class="mt-2 text-[13px] leading-6 text-[#8b998d]">当前删除只影响本次页面会话，不会删除数据库记录。刷新后会重新从数据库读取。</p>
        <div class="mt-7 flex justify-end gap-2"><button type="button" class="rounded-full border border-[#e3e8e1] px-5 py-2.5 text-[13px] text-[#6f8171]" @click="confirmDelete = false">取消</button><button type="button" class="rounded-full bg-[#b8796f] px-5 py-2.5 text-[13px] font-semibold text-white hover:bg-[#a5685f]" @click="remove">确认删除</button></div>
      </div>
    </div>

    <div v-if="viewerImage" ref="viewerDialog" tabindex="-1" role="dialog" aria-modal="true" aria-label="图片查看" class="fixed inset-0 z-[60] flex flex-col bg-[#17231b]/95 text-white" @click.self="closeViewer">
      <div class="flex items-center justify-between gap-4 px-4 py-4 sm:px-7">
        <div class="min-w-0 text-sm"><span class="font-semibold">{{ viewerIndex + 1 }} / {{ note.images.length }}</span><span class="ml-3 truncate text-white/65">{{ viewerImage.originalName }}</span></div>
        <button type="button" class="rounded-full p-2 hover:bg-white/15" aria-label="关闭图片查看" @click="closeViewer"><X :size="22" /></button>
      </div>
      <div class="relative min-h-0 flex-1 px-12 sm:px-16">
        <div class="h-full w-full overflow-auto rounded-lg bg-black/20">
          <div class="grid h-full place-items-center" :style="{ width: `${viewerZoom * 100}%`, height: `${viewerZoom * 100}%` }">
            <img :key="viewerImage.id" :src="viewerImage.url" :alt="viewerImage.originalName" class="h-full w-full object-contain" />
          </div>
        </div>
        <button v-if="note.images.length > 1" type="button" class="absolute left-2 top-1/2 -translate-y-1/2 rounded-full p-2 hover:bg-white/15 disabled:opacity-30 sm:left-5" aria-label="上一张图片" :disabled="viewerIndex === 0" @click="showImage(-1)"><ChevronLeft :size="26" /></button>
        <button v-if="note.images.length > 1" type="button" class="absolute right-2 top-1/2 -translate-y-1/2 rounded-full p-2 hover:bg-white/15 disabled:opacity-30 sm:right-5" aria-label="下一张图片" :disabled="viewerIndex === note.images.length - 1" @click="showImage(1)"><ChevronRight :size="26" /></button>
      </div>
      <div class="flex items-center justify-center gap-5 px-4 py-5 text-sm">
        <button type="button" class="rounded-full p-2 hover:bg-white/15 disabled:opacity-30" aria-label="缩小图片" :disabled="viewerZoom === 1" @click="zoomImage(-0.25)"><Minus :size="19" /></button>
        <span class="min-w-12 text-center">{{ Math.round(viewerZoom * 100) }}%</span>
        <button type="button" class="rounded-full p-2 hover:bg-white/15 disabled:opacity-30" aria-label="放大图片" :disabled="viewerZoom === 3" @click="zoomImage(0.25)"><Plus :size="19" /></button>
      </div>
    </div>
  </main>
</template>
