<template>
  <header class="fixed top-0 left-0 right-0 z-50 border-b border-border bg-bg-primary">
    <div class="flex items-center h-14 px-6 gap-8">
      <!-- Logo -->
      <RouterLink to="/" class="flex items-center gap-2 font-bold text-lg text-toss-blue shrink-0">
        <div class="w-7 h-7 bg-toss-blue rounded-md flex items-center justify-center text-white text-xs font-black">C</div>
        <span>ChartAnalysis</span>
      </RouterLink>

      <!-- Nav links -->
      <nav class="flex items-center gap-6">
        <RouterLink to="/" class="nav-link" :class="{ active: route.name === 'home' }">홈</RouterLink>
        <RouterLink to="/" class="nav-link">주식 골라보기</RouterLink>
        <RouterLink to="/portfolio" class="nav-link" :class="{ active: route.name === 'portfolio' }">내 계좌</RouterLink>
      </nav>

      <!-- Search -->
      <div class="flex-1 max-w-xs">
        <div class="flex items-center gap-2 bg-bg-secondary border border-border rounded-lg px-3 py-2 text-sm text-text-muted">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
          </svg>
          <span>/ 를 눌러 검색하세요</span>
        </div>
      </div>

      <!-- User -->
      <div class="ml-auto flex items-center gap-4">
        <span class="text-sm text-text-secondary">{{ authStore.user?.nickname }}</span>
        <button @click="handleLogout" class="text-sm text-text-muted hover:text-text-primary transition-colors">
          로그아웃
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRoute, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.nav-link {
  @apply text-sm text-text-secondary hover:text-text-primary transition-colors font-medium;
}
.nav-link.active {
  @apply text-text-primary;
}
</style>
