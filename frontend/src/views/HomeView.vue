<template>
  <div class="flex flex-col h-screen">
    <NavBar />
    <MarketBar class="mt-14" />

    <!-- Main 3-column layout -->
    <div class="flex flex-1 overflow-hidden">
      <!-- LEFT: Stock list (300px) -->
      <aside class="w-[300px] border-r border-border flex flex-col shrink-0 overflow-hidden">
        <StockList />
      </aside>

      <!-- CENTER: Chart -->
      <main class="flex-1 flex flex-col overflow-hidden">
        <!-- Stock header -->
        <div v-if="stockStore.selectedStock" class="px-6 py-4 border-b border-border shrink-0">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 bg-bg-secondary rounded-full flex items-center justify-center text-sm font-bold">
              {{ stockStore.selectedStock.symbol.slice(0, 2) }}
            </div>
            <div>
              <h2 class="text-xl font-bold">{{ stockStore.selectedStock.name }}</h2>
              <div class="flex items-center gap-3 mt-0.5">
                <span class="text-2xl font-bold" :class="priceColor(stockStore.selectedStock.changeRate)">
                  {{ formatPrice(stockStore.selectedStock.currentPrice, stockStore.selectedStock.market) }}
                </span>
                <span class="text-base" :class="priceColor(stockStore.selectedStock.changeRate)">
                  {{ formatChangeRate(stockStore.selectedStock.changeRate) }}
                </span>
              </div>
            </div>
            <div class="ml-auto flex items-center gap-2">
              <span class="text-xs text-text-muted bg-bg-secondary px-2 py-1 rounded">{{ stockStore.selectedStock.market }}</span>
              <span class="text-xs text-text-muted bg-bg-secondary px-2 py-1 rounded">실시간</span>
            </div>
          </div>
        </div>
        <div v-else class="px-6 py-4 border-b border-border shrink-0 text-text-muted">
          좌측에서 종목을 선택하세요
        </div>

        <!-- Candlestick chart -->
        <div class="flex-1 p-4">
          <CandlestickChart class="w-full h-full" />
        </div>

        <!-- News section -->
        <div v-if="stockStore.selectedStock" class="border-t border-border px-6 py-4 shrink-0">
          <h3 class="text-sm font-semibold text-text-secondary mb-3">왜 올랐을까?</h3>
          <div class="flex gap-4 overflow-x-auto">
            <div
              v-for="news in mockNews"
              :key="news.id"
              class="shrink-0 w-48 p-3 bg-bg-secondary rounded-xl border border-border"
            >
              <div class="text-xs text-toss-blue font-semibold mb-1">{{ news.tag }}</div>
              <div class="text-sm text-text-primary leading-relaxed">{{ news.title }}</div>
              <div class="text-xs text-text-muted mt-2">{{ news.time }}</div>
            </div>
          </div>
        </div>
      </main>

      <!-- RIGHT: Portfolio sidebar (280px) -->
      <aside class="w-[280px] border-l border-border flex flex-col shrink-0 overflow-hidden">
        <PortfolioSidebar />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, computed } from 'vue'
import NavBar from '@/components/layout/NavBar.vue'
import MarketBar from '@/components/layout/MarketBar.vue'
import StockList from '@/components/stock/StockList.vue'
import CandlestickChart from '@/components/stock/CandlestickChart.vue'
import PortfolioSidebar from '@/components/portfolio/PortfolioSidebar.vue'
import { useStockStore } from '@/stores/stock'
import { formatPrice, formatChangeRate } from '@/utils/format'

const stockStore = useStockStore()

const mockNews = computed(() => {
  const stock = stockStore.selectedStock
  if (!stock) return []
  return [
    { id: 1, tag: '기관 매수세', title: `기관 투자자가 ${stock.name} 6일 연속 사고 있어요.`, time: '1일 전' },
    { id: 2, tag: '외국인 순매수', title: `외국인이 많이 산 Top 10 종목이에요.`, time: '2일 전' },
    { id: 3, tag: 'AI 분석', title: `${stock.name} 실적 발표 앞두고 기대감 상승 중`, time: '3일 전' },
  ]
})

function priceColor(rate) {
  const n = Number(rate)
  if (n > 0) return 'text-up'
  if (n < 0) return 'text-down'
  return 'text-text-primary'
}

onMounted(() => {
  stockStore.connectWebSocket()
})

onUnmounted(() => {
  stockStore.disconnectWebSocket()
})
</script>
