<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { BookOpenText, Plus, Sparkles } from '@lucide/vue'
</script>

<template>
  <div class="min-h-screen bg-[#f8f7f2] text-[#303b33]">
    <header class="sticky top-0 z-30 border-b border-[#e8e9dd] bg-[#f8f7f2]/90 backdrop-blur-xl">
      <div class="mx-auto flex h-[76px] max-w-[1200px] items-center justify-between px-5 sm:px-8">
        <RouterLink to="/notes" class="group flex items-center gap-3" aria-label="拾日首页">
          <span class="flex size-10 items-center justify-center rounded-[14px] bg-[#75856b] text-white shadow-sm transition-transform group-hover:-rotate-6">
            <BookOpenText :size="21" :stroke-width="1.8" />
          </span>
          <span class="leading-none">
            <span class="block text-[21px] font-semibold tracking-[0.14em]">拾日</span>
            <span class="mt-1.5 block text-[9px] font-medium uppercase tracking-[0.22em] text-[#9ba59a]">A little every day</span>
          </span>
        </RouterLink>

        <nav class="flex items-center gap-3 sm:gap-7" aria-label="主导航">
          <RouterLink to="/notes" class="hidden text-[13px] font-medium text-[#718076] transition-colors hover:text-[#394b3c] sm:block">所有记录</RouterLink>
          <span class="hidden h-4 w-px bg-[#d9ded2] sm:block" aria-hidden="true"></span>
          <RouterLink to="/notes/new" class="inline-flex items-center gap-2 rounded-full bg-[#3c5141] px-4 py-2.5 text-xs font-semibold text-white shadow-[0_4px_12px_rgba(60,81,65,0.12)] transition hover:-translate-y-0.5 hover:bg-[#304333] sm:px-5 sm:text-[13px]">
            <Plus :size="16" :stroke-width="2" />
            写一篇记录
          </RouterLink>
        </nav>
      </div>
    </header>

    <RouterView />

    <footer class="mx-auto mt-12 flex max-w-[1200px] flex-col gap-4 border-t border-[#e7e9df] px-5 py-8 text-xs text-[#a0a99e] sm:flex-row sm:items-center sm:justify-between sm:px-8">
      <p class="flex items-center gap-2"><Sparkles :size="14" /> 把平凡的日子，认真地收藏。</p>
      <p>拾日 · 前端交互原型</p>
    </footer>
  </div>
</template>
