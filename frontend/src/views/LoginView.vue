<template>
  <div class="min-h-screen bg-bg-primary flex items-center justify-center px-4">
    <div class="w-full max-w-sm">
      <!-- Logo -->
      <div class="text-center mb-10">
        <div class="inline-flex items-center gap-3 mb-4">
          <div class="w-10 h-10 bg-toss-blue rounded-xl flex items-center justify-center text-white font-black text-lg">C</div>
          <span class="text-2xl font-bold">ChartAnalysis</span>
        </div>
        <p class="text-text-secondary text-sm">실시간 모의투자 플랫폼</p>
      </div>

      <div class="card p-8">
        <h1 class="text-xl font-bold mb-6">로그인</h1>

        <form @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="text-sm text-text-secondary mb-2 block">이메일</label>
            <input
              v-model="form.email"
              type="email"
              placeholder="example@email.com"
              class="input-field"
              required
            />
          </div>
          <div>
            <label class="text-sm text-text-secondary mb-2 block">비밀번호</label>
            <input
              v-model="form.password"
              type="password"
              placeholder="••••••••"
              class="input-field"
              required
            />
          </div>

          <div v-if="error" class="p-3 bg-red-900/30 border border-red-500/30 rounded-lg text-red-400 text-sm">
            {{ error }}
          </div>

          <button type="submit" :disabled="isLoading" class="btn-primary w-full py-3 mt-2">
            {{ isLoading ? '로그인 중...' : '로그인' }}
          </button>
        </form>

        <p class="text-center text-sm text-text-muted mt-6">
          계정이 없으신가요?
          <RouterLink to="/register" class="text-toss-blue hover:underline ml-1">회원가입</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = ref({ email: '', password: '' })
const isLoading = ref(false)
const error = ref('')

async function handleLogin() {
  error.value = ''
  isLoading.value = true
  try {
    await authStore.login(form.value.email, form.value.password)
    router.push({ name: 'home' })
  } catch (e) {
    error.value = e.response?.data?.message || '이메일 또는 비밀번호가 올바르지 않습니다.'
  } finally {
    isLoading.value = false
  }
}
</script>
