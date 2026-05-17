<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/60" @click.self="$emit('close')">
    <div class="bg-bg-card border border-border rounded-2xl w-96 p-6">
      <!-- Header -->
      <div class="flex justify-between items-center mb-6">
        <h3 class="text-lg font-bold">
          <span :class="type === 'BUY' ? 'text-up' : 'text-down'">{{ type === 'BUY' ? '매수' : '매도' }}</span>
          · {{ stock?.name }}
        </h3>
        <button @click="$emit('close')" class="text-text-muted hover:text-text-primary">✕</button>
      </div>

      <!-- Current price -->
      <div class="flex justify-between items-center mb-4 p-3 bg-bg-secondary rounded-lg">
        <span class="text-sm text-text-secondary">현재가</span>
        <span class="font-bold text-lg">{{ formatPrice(stock?.currentPrice, stock?.market) }}</span>
      </div>

      <!-- Quantity input -->
      <div class="mb-4">
        <label class="text-sm text-text-secondary mb-2 block">수량 (주)</label>
        <div class="flex items-center gap-2">
          <button @click="quantity = Math.max(1, quantity - 1)" class="w-10 h-10 bg-bg-secondary rounded-lg text-lg hover:bg-bg-tertiary">-</button>
          <input
            v-model.number="quantity"
            type="number"
            min="1"
            class="input-field text-center flex-1"
          />
          <button @click="quantity++" class="w-10 h-10 bg-bg-secondary rounded-lg text-lg hover:bg-bg-tertiary">+</button>
        </div>
      </div>

      <!-- Total amount -->
      <div class="mb-4 p-3 bg-bg-secondary rounded-lg">
        <div class="flex justify-between text-sm">
          <span class="text-text-secondary">주문 금액</span>
          <span class="font-bold">{{ formatAmount(totalAmount) }}</span>
        </div>
        <div class="flex justify-between text-xs text-text-muted mt-1">
          <span>{{ type === 'BUY' ? '주문 가능 금액' : '보유 수량' }}</span>
          <span>{{ type === 'BUY' ? formatAmount(portfolio?.availableCash) : `${availableQuantity}주` }}</span>
        </div>
      </div>

      <!-- Error message -->
      <div v-if="error" class="mb-4 p-3 bg-red-900/30 border border-red-500/30 rounded-lg text-red-400 text-sm">
        {{ error }}
      </div>

      <!-- Confirm button -->
      <button
        @click="executeTrade"
        :disabled="isLoading"
        class="w-full py-3 rounded-xl font-bold text-white transition-opacity disabled:opacity-50"
        :class="type === 'BUY' ? 'bg-up' : 'bg-down'"
      >
        {{ isLoading ? '처리 중...' : (type === 'BUY' ? '매수하기' : '매도하기') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useStockStore } from '@/stores/stock'
import { usePortfolioStore } from '@/stores/portfolio'
import { formatPrice, formatAmount } from '@/utils/format'

const props = defineProps({
  type: { type: String, required: true }
})
const emit = defineEmits(['close', 'success'])

const stockStore = useStockStore()
const portfolioStore = usePortfolioStore()

const stock = computed(() => stockStore.selectedStock)
const portfolio = computed(() => portfolioStore.portfolio)
const quantity = ref(1)
const isLoading = ref(false)
const error = ref('')

const totalAmount = computed(() => {
  if (!stock.value?.currentPrice) return 0
  return Number(stock.value.currentPrice) * quantity.value
})

const availableQuantity = computed(() => {
  const holding = portfolio.value?.holdings?.find(h => h.symbol === stock.value?.symbol)
  return holding?.quantity || 0
})

async function executeTrade() {
  if (!stock.value) return
  error.value = ''
  isLoading.value = true
  try {
    await portfolioStore.executeTrade(stock.value.symbol, quantity.value, props.type)
    emit('success')
  } catch (e) {
    error.value = e.response?.data?.message || '거래 실패. 다시 시도해주세요.'
  } finally {
    isLoading.value = false
  }
}
</script>
