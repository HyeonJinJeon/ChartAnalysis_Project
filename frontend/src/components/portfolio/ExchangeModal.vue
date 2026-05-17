<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/60" @click.self="$emit('close')">
    <div class="bg-bg-card border border-border rounded-2xl w-96 p-6">
      <!-- Header -->
      <div class="flex justify-between items-center mb-6">
        <h3 class="text-lg font-bold">환전</h3>
        <button @click="$emit('close')" class="text-text-muted hover:text-text-primary">✕</button>
      </div>

      <!-- Exchange rate -->
      <div class="mb-4 p-3 bg-bg-secondary rounded-lg text-sm">
        <div class="flex justify-between">
          <span class="text-text-secondary">현재 환율</span>
          <span class="font-semibold">
            1 USD = {{ exchangeRate ? Number(exchangeRate.rate).toLocaleString('ko-KR', { maximumFractionDigits: 2 }) : '...' }}원
          </span>
        </div>
      </div>

      <!-- Direction selector -->
      <div class="mb-4">
        <label class="text-sm text-text-secondary mb-2 block">환전 방향</label>
        <div class="flex gap-2">
          <button
            @click="direction = 'KRW_TO_USD'"
            class="flex-1 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="direction === 'KRW_TO_USD' ? 'bg-toss-blue text-white' : 'bg-bg-secondary text-text-secondary'"
          >
            원화 → 달러
          </button>
          <button
            @click="direction = 'USD_TO_KRW'"
            class="flex-1 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="direction === 'USD_TO_KRW' ? 'bg-toss-blue text-white' : 'bg-bg-secondary text-text-secondary'"
          >
            달러 → 원화
          </button>
        </div>
      </div>

      <!-- Amount input -->
      <div class="mb-4">
        <label class="text-sm text-text-secondary mb-2 block">
          {{ direction === 'KRW_TO_USD' ? '원화 금액 (₩)' : '달러 금액 ($)' }}
        </label>
        <input
          v-model.number="amount"
          type="number"
          min="0"
          step="direction === 'KRW_TO_USD' ? 1000 : 1"
          class="input-field w-full"
          :placeholder="direction === 'KRW_TO_USD' ? '원화 입력' : '달러 입력'"
        />
        <div class="text-xs text-text-muted mt-1">
          보유: {{ direction === 'KRW_TO_USD'
            ? `${Number(portfolio?.availableCash || 0).toLocaleString('ko-KR')}원`
            : `$${Number(portfolio?.availableUsd || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
          }}
        </div>
      </div>

      <!-- Result -->
      <div v-if="amount > 0 && exchangeRate" class="mb-4 p-3 bg-bg-secondary rounded-lg">
        <div class="flex justify-between text-sm">
          <span class="text-text-secondary">받는 금액 (예상)</span>
          <span class="font-bold text-toss-blue">
            {{ direction === 'KRW_TO_USD'
              ? `$${resultAmount.toLocaleString('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 4 })}`
              : `${resultAmount.toLocaleString('ko-KR')}원`
            }}
          </span>
        </div>
      </div>

      <!-- Error -->
      <div v-if="error" class="mb-4 p-3 bg-red-900/30 border border-red-500/30 rounded-lg text-red-400 text-sm">
        {{ error }}
      </div>

      <!-- Confirm -->
      <button
        @click="executeExchange"
        :disabled="isLoading || !amount || amount <= 0"
        class="w-full py-3 rounded-xl font-bold text-white bg-toss-blue transition-opacity disabled:opacity-50"
      >
        {{ isLoading ? '처리 중...' : '환전하기' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { usePortfolioStore } from '@/stores/portfolio'

const emit = defineEmits(['close', 'success'])

const portfolioStore = usePortfolioStore()
const portfolio = computed(() => portfolioStore.portfolio)
const exchangeRate = computed(() => portfolioStore.exchangeRate)

const direction = ref('KRW_TO_USD')
const amount = ref(0)
const isLoading = ref(false)
const error = ref('')

const resultAmount = computed(() => {
  if (!amount.value || !exchangeRate.value?.rate) return 0
  const rate = Number(exchangeRate.value.rate)
  if (direction.value === 'KRW_TO_USD') {
    return amount.value / rate
  } else {
    return amount.value * rate
  }
})

async function executeExchange() {
  if (!amount.value || amount.value <= 0) return
  error.value = ''
  isLoading.value = true
  try {
    const fromCurrency = direction.value === 'KRW_TO_USD' ? 'KRW' : 'USD'
    await portfolioStore.exchangeCurrency(fromCurrency, amount.value)
    emit('success')
  } catch (e) {
    error.value = e.response?.data?.message || '환전 실패. 다시 시도해주세요.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  portfolioStore.fetchExchangeRate()
})
</script>
