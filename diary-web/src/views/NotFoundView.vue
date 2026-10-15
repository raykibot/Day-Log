<script setup>
import { RouterLink } from 'vue-router'
import { ArrowLeft, Compass } from '@lucide/vue'
</script>

<template>
  <main class="mx-auto flex min-h-[65vh] max-w-[900px] flex-col items-center justify-center px-5 text-center">
    <span class="flex size-16 items-center justify-center rounded-[22px] bg-[#eaf0e7] text-[#789079]"><Compass :size="30" :stroke-width="1.6" /></span>
    <p class="mt-6 text-xs font-semibold tracking-[0.25em] text-[#9aab99]">PAGE NOT FOUND</p>
    <h1 class="mt-3 text-[32px] font-semibold text-[#3c5141]">这条路上，暂时没有记录。</h1>
    <p class="mt-3 text-sm text-[#8c9a8d]">检查一下地址，或回到首页继续浏览。</p>
    <RouterLink to="/notes" class="mt-7 inline-flex items-center gap-2 rounded-full bg-[#455e4b] px-5 py-3 text-[13px] font-semibold text-white"><ArrowLeft :size="16" /> 返回所有记录</RouterLink>
  </main>
</template>
