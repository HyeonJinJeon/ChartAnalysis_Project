<template>
  <div class="min-h-screen bg-bg-primary flex items-center justify-center px-4">
    <div class="w-full max-w-sm">
      <div class="text-center mb-10">
        <div class="inline-flex items-center gap-3 mb-4">
          <div class="w-10 h-10 bg-toss-blue rounded-xl flex items-center justify-center text-white font-black text-lg">C</div>
          <span class="text-2xl font-bold">ChartAnalysis</span>
        </div>
        <p class="text-text-secondary text-sm">실시간 모의투자 플랫폼</p>
      </div>

      <div class="card p-8">
        <h1 class="text-xl font-bold mb-2">회원가입</h1>
        <p class="text-sm text-text-muted mb-6">가입 즉시 100만원 시드머니 지급!</p>

        <form @submit.prevent="handleRegister" class="space-y-4">
          <div>
            <label class="text-sm text-text-secondary mb-2 block">닉네임</label>
            <input v-model="form.nickname" type="text" placeholder="닉네임 입력" class="input-field" required />
          </div>
          <div>
            <label class="text-sm text-text-secondary mb-2 block">이메일</label>
            <input v-model="form.email" type="email" placeholder="example@email.com" class="input-field" required />
          </div>
          <div>
            <label class="text-sm text-text-secondary mb-2 block">비밀번호</label>
            <input v-model="form.password" type="password" placeholder="8자 이상" class="input-field" required />
          </div>

          <div v-if="error" class="p-3 bg-red-900/30 border border-red-500/30 rounded-lg text-red-400 text-sm">
            {{ error }}
          </div>

          <button type="submit" :disabled="isLoading" class="btn-primary w-full py-3 mt-2">
            {{ isLoading ? '가입 중...' : '회원가입' }}
          </button>
        </form>

        <p class="text-center text-sm text-text-muted mt-6">
          이미 계정이 있으신가요?
          <RouterLink to="/login" class="text-toss-blue hover:underline ml-1">로그인</RouterLink>
        </p>
      </div>

      <!-- Seed money notice -->
      <div class="mt-4 p-4 bg-toss-blue/10 border border-toss-blue/20 rounded-xl text-center">
        <p class="text-sm text-toss-blue font-semibold">💰 매월 1일 100만원 시드머니 지급</p>
        <p class="text-xs text-text-muted mt-1">모의투자로 투자 실력을 키워보세요</p>
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

const form = ref({ email: '', password: '', nickname: '' })
const isLoading = ref(false)
const error = ref('')

async function handleRegister() {
  error.value = ''
  isLoading.value = true
  try {
    await authStore.register(form.value.email, form.value.password, form.value.nickname)
    router.push({ name: 'home' })
  } catch (e) {
    error.value = e.response?.data?.message || '회원가입에 실패했습니다. 다시 시도해주세요.'
  } finally {
    isLoading.value = false
  }
}
</script>
