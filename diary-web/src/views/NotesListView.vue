<script setup>
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight, CalendarDays, Feather, FileText, Leaf, Plus, Sparkles } from '@lucide/vue'
import { apiErrorMessage } from '@/api/http'
import { notesRepository } from '@/data/notesRepository'
import { CATEGORY_LABELS, displayTitle, excerpt, formatDate } from '@/lib/notes'

const filter = ref('ALL')
const notes = ref([])
const total = ref(0)
const loading = ref(true)
const loadError = ref('')
let loadVersion = 0
const today = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }).format(new Date())

async function load() {
  const version = ++loadVersion
  loading.value = true
  loadError.value = ''
  try {
    const category = filter.value === 'ALL' ? undefined : filter.value
    const all = await notesRepository.list({ category })
    if (version !== loadVersion) return
    total.value = all.length
    notes.value = all
  } catch (error) {
    if (version !== loadVersion) return
    notes.value = []
    loadError.value = apiErrorMessage(error)
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

watch(filter, load, { immediate: true })
</script>

<template>
  <main class="mx-auto max-w-[1200px] px-5 pb-10 pt-10 sm:px-8 sm:pt-14">
    <section class="relative overflow-hidden rounded-[30px] border border-[#dfe8da] bg-[#edf2e9] px-7 py-9 sm:px-11 sm:py-12 lg:px-14">
      <div class="pointer-events-none absolute -right-16 -top-28 size-[330px] rounded-full border-[54px] border-white/25 sm:right-12 sm:top-[-170px] sm:size-[470px] sm:border-[75px]"></div>
      <div class="pointer-events-none absolute -bottom-32 right-32 size-[270px] rounded-full bg-[#dce8d7]/70 blur-[2px]"></div>
      <div class="relative z-10 max-w-[630px]">
        <p class="mb-5 inline-flex items-center gap-2 rounded-full border border-[#d5e0d0] bg-white/65 px-3 py-1.5 text-[11px] font-semibold tracking-[0.08em] text-[#708773]"><Sparkles :size="13" /> {{ today }}</p>
        <h1 class="text-[34px] font-semibold leading-[1.35] tracking-[-0.04em] text-[#344b3c] sm:text-[46px]">把日子，<br class="sm:hidden">慢慢记下来<span class="text-[#a7bca4]">。</span></h1>
        <p class="mt-5 max-w-[510px] text-[14px] leading-8 text-[#788a7c] sm:text-[15px]">工作中的一点收获，生活里的片刻心动。都值得拥有一个安静的地方，好好收藏。</p>
        <RouterLink to="/notes/new" class="mt-8 inline-flex items-center gap-2 rounded-full bg-[#425b48] px-5 py-3 text-[13px] font-semibold text-white transition hover:-translate-y-0.5 hover:bg-[#344d3b]">
          <Feather :size="16" /> 开始记录 <ArrowRight :size="15" />
        </RouterLink>
      </div>
      <div class="pointer-events-none absolute bottom-8 right-10 hidden rotate-[-8deg] rounded-[22px] border border-white/80 bg-[#fffcf6] p-5 shadow-[0_18px_42px_rgba(88,107,83,0.11)] lg:block" aria-hidden="true">
        <div class="mb-5 h-2 w-20 rounded-full bg-[#e8eadd]"></div>
        <div class="mb-2 h-2 w-36 rounded-full bg-[#dce5d9]"></div>
        <div class="mb-2 h-2 w-32 rounded-full bg-[#e8eadd]"></div>
        <div class="h-2 w-24 rounded-full bg-[#e8eadd]"></div>
        <div class="mt-7 flex items-center justify-between text-[#b7c8af]"><Leaf :size="18" /><span class="text-[10px] tracking-widest">DAY 01</span></div>
      </div>
    </section>

    <section class="mt-13 sm:mt-16" aria-labelledby="notes-heading">
      <div class="flex flex-col gap-6 border-b border-[#e5e9df] pb-6 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.22em] text-[#91a393]">YOUR COLLECTION</p>
          <div class="flex items-baseline gap-3"><h2 id="notes-heading" class="text-[26px] font-semibold tracking-[-0.04em] text-[#34453a] sm:text-[30px]">我的记录</h2><span class="text-[13px] text-[#9aa69a]">共 {{ total }} 篇</span></div>
          <p class="mt-2 text-xs text-[#9aa69a]">新增记录写入数据库；编辑和删除暂时只影响当前页面会话。</p>
        </div>
        <div class="inline-flex w-fit rounded-full border border-[#e3e8dd] bg-white p-1" role="group" aria-label="按分类筛选">
          <button v-for="item in [{ key: 'ALL', label: '全部' }, { key: 'WORK', label: '工作' }, { key: 'LIFE', label: '生活' }]" :key="item.key" type="button" :aria-pressed="filter === item.key" class="rounded-full px-4 py-2 text-[12px] font-medium transition sm:px-5" :class="filter === item.key ? 'bg-[#49614e] text-white shadow-sm' : 'text-[#7e8d7f] hover:text-[#3e5443]'" @click="filter = item.key">{{ item.label }}</button>
        </div>
      </div>

      <div v-if="loading" role="status" class="mt-7 rounded-[24px] border border-[#e8e9e1] bg-white px-6 py-16 text-center text-sm text-[#879789]">正在读取记录…</div>
      <div v-else-if="loadError" role="alert" class="mt-7 rounded-[24px] border border-[#f0d4cb] bg-[#fff7f3] px-6 py-10 text-center text-sm text-[#ab6658]">
        <p>{{ loadError }}</p><button type="button" class="mt-4 font-semibold underline" @click="load">重试</button>
      </div>
      <div v-else-if="notes.length" class="mt-7 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <RouterLink v-for="(note, index) in notes" :key="note.id" :to="`/notes/${note.id}`" class="group relative flex min-h-[260px] flex-col overflow-hidden rounded-[23px] border border-[#e8e9e1] bg-white p-6 transition duration-300 hover:-translate-y-1 hover:border-[#ced9cb] hover:shadow-[0_20px_45px_rgba(53,71,52,0.09)]" :class="index === 0 && filter === 'ALL' ? 'md:col-span-2 lg:col-span-1' : ''">
          <div class="mb-8 flex items-start justify-between">
            <span class="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-[11px] font-semibold" :class="note.category === 'WORK' ? 'bg-[#ebf1eb] text-[#67816b]' : 'bg-[#f8eee4] text-[#ba8d69]'">
              <FileText v-if="note.category === 'WORK'" :size="12" /><Leaf v-else :size="12" />{{ CATEGORY_LABELS[note.category] }}
            </span>
            <span class="flex size-8 items-center justify-center rounded-full border border-[#edf0e9] text-[#9ba99b] transition group-hover:border-[#6d856f] group-hover:bg-[#6d856f] group-hover:text-white"><ArrowRight :size="15" /></span>
          </div>
          <h3 class="line-clamp-2 text-[19px] font-semibold leading-7 tracking-[-0.02em] text-[#3d4d40]">{{ displayTitle(note) }}</h3>
          <p class="mt-3 line-clamp-3 text-[13px] leading-[1.85] text-[#89958a]">{{ excerpt(note.content) }}</p>
          <div v-if="note.images?.length" class="mb-5 mt-5 grid grid-cols-3 gap-2" aria-label="记录图片预览">
            <div v-for="(image, imageIndex) in note.images.slice(0, 3)" :key="image.id" class="relative overflow-hidden rounded-xl bg-[#f4f6f1]">
              <img :src="image.url" :alt="image.originalName" loading="lazy" class="aspect-square w-full object-cover" />
              <span v-if="imageIndex === 2 && note.images.length > 3" class="absolute inset-0 flex items-center justify-center bg-[#283a2e]/60 text-sm font-semibold text-white">+{{ note.images.length - 3 }}</span>
            </div>
          </div>
          <div class="mt-auto flex items-center justify-between border-t border-[#f0f2ed] pt-5 text-[11px] text-[#a1aca1]"><span class="flex items-center gap-1.5"><CalendarDays :size="13" />{{ formatDate(note.createTime) }}</span><span class="truncate pl-4">{{ note.mdName }}</span></div>
        </RouterLink>
      </div>

      <div v-else class="mt-7 flex min-h-[300px] flex-col items-center justify-center rounded-[24px] border border-dashed border-[#d9e1d6] bg-white/70 px-5 text-center">
        <span class="mb-4 flex size-14 items-center justify-center rounded-2xl bg-[#eff3ec] text-[#829b82]"><Feather :size="25" /></span>
        <h3 class="text-lg font-semibold text-[#415345]">这里还没有记录</h3>
        <p class="mt-2 text-sm text-[#96a397]">换个分类看看，或写下第一篇记录。</p>
        <RouterLink to="/notes/new" class="mt-5 inline-flex items-center gap-1.5 text-[13px] font-semibold text-[#58745e]"><Plus :size="15" /> 写一篇记录</RouterLink>
      </div>
    </section>
  </main>
</template>
