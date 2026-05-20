<template>
  <div class="flex flex-col h-full">
    <!-- Tabs -->
    <div class="flex border-b border-border px-4 shrink-0">
      <button
        v-for="tab in tabs"
        :key="tab"
        @click="stockStore.setFilter(tab)"
        class="px-3 py-3 text-sm font-medium border-b-2 -mb-px transition-colors"
        :class="stockStore.activeFilter === tab
          ? 'border-text-primary text-text-primary'
          : 'border-transparent text-text-muted hover:text-text-secondary'"
      >
        {{ tab }}
      </button>
    </div>

    <!-- Column headers -->
    <div class="flex items-center px-4 py-2 text-xs text-text-muted border-b border-border shrink-0">
      <span class="w-6 text-center">순위</span>
      <span class="flex-1 ml-3">종목명</span>
      <span class="text-right w-24">현재가</span>
      <span class="text-right w-16">등락률</span>
    </div>

    <!-- Stock list -->
    <div class="flex-1 overflow-y-auto">
      <div
        v-for="(stock, index) in stockStore.filteredStocks"
        :key="stock.symbol"
        @click="stockStore.selectStock(stock.symbol)"
        class="flex items-center px-4 py-3 cursor-pointer hover:bg-bg-secondary transition-colors"
        :class="{ 'bg-bg-secondary': stockStore.selectedStock?.symbol === stock.symbol }"
      >
        <span class="w-6 text-center text-xs text-text-muted">{{ index + 1 }}</span>
        <div class="flex-1 ml-3 min-w-0">
          <div class="font-medium text-sm truncate">{{ stock.name }}</div>
          <div class="text-xs text-text-muted">{{ stock.symbol }} · {{ stock.market }}</div>
        </div>
        <div class="text-right w-24">
          <div class="text-sm font-semibold" :class="priceColor(stock.changeRate)">
            {{ formatPrice(stock.currentPrice, stock.market) }}
          </div>
        </div>
        <div class="text-right w-16">
          <span class="text-xs font-medium" :class="priceColor(stock.changeRate)">
            {{ formatChangeRate(stock.changeRate) }}
          </span>
        </div>
      </div>

      <LoadingSpinner v-if="stockStore.isLoading" />
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useStockStore } from '@/stores/stock'
import { formatPrice, formatChangeRate } from '@/utils/format'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const stockStore = useStockStore()
const tabs = ['전체', '국내', '해외']

function priceColor(rate) {
  const n = Number(rate)
  if (n > 0) return 'text-up'
  if (n < 0) return 'text-down'
  return 'text-text-primary'
}

onMounted(async () => {
  await stockStore.fetchStocks()
  // Subscribe to all stocks for live price updates now that list is loaded
  stockStore.subscribeToAllStocks()
  if (stockStore.stocks.length > 0 && !stockStore.selectedStock) {
    stockStore.selectStock(stockStore.stocks[0].symbol)
  }
})
</script>
