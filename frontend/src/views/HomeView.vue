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
        <div class="flex-1 min-h-0 p-4">
          <CandlestickChart class="w-full h-full" />
        </div>

        <!-- Stock info + news panel -->
        <div class="h-[260px] shrink-0">
          <StockInfoPanel
            v-if="stockStore.selectedStock"
            :symbol="stockStore.selectedStock.symbol"
          />
          <div v-else class="h-full border-t border-border flex items-center justify-center text-text-muted text-sm">
            종목을 선택하면 기업정보와 뉴스를 확인할 수 있어요
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
import { onMounted, onUnmounted } from 'vue'
import NavBar from '@/components/layout/NavBar.vue'
import MarketBar from '@/components/layout/MarketBar.vue'
import StockList from '@/components/stock/StockList.vue'
import CandlestickChart from '@/components/stock/CandlestickChart.vue'
import StockInfoPanel from '@/components/stock/StockInfoPanel.vue'
import PortfolioSidebar from '@/components/portfolio/PortfolioSidebar.vue'
import { useStockStore } from '@/stores/stock'
import { formatPrice, formatChangeRate } from '@/utils/format'

const stockStore = useStockStore()

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
