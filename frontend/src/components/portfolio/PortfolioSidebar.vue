<template>
  <div class="flex flex-col h-full">
    <!-- Header -->
    <div class="px-4 py-4 border-b border-border">
      <div class="flex items-center justify-between mb-1">
        <span class="text-sm text-text-secondary font-medium">내 투자</span>
        <RouterLink to="/portfolio" class="text-xs text-toss-blue">전체보기</RouterLink>
      </div>
      <div v-if="portfolio">
        <div class="text-2xl font-bold mt-1">
          {{ formatAmount(totalPortfolioValue) }}
        </div>
        <div class="text-sm mt-0.5" :class="portfolio.totalProfitLoss >= 0 ? 'text-up' : 'text-down'">
          {{ portfolio.totalProfitLoss >= 0 ? '+' : '' }}{{ formatAmount(portfolio.totalProfitLoss) }}
          ({{ formatChangeRate(portfolio.totalProfitLossRate) }})
        </div>
      </div>
      <div v-else class="text-text-muted text-sm mt-2">로딩 중...</div>
    </div>

    <!-- Available cash -->
    <div v-if="portfolio" class="px-4 py-3 border-b border-border">
      <div class="flex justify-between items-center text-sm">
        <span class="text-text-secondary">주문 가능 금액</span>
        <span class="font-semibold">{{ formatAmount(portfolio.availableCash) }}</span>
      </div>
    </div>

    <!-- Trade buttons for selected stock -->
    <div v-if="stockStore.selectedStock" class="px-4 py-3 border-b border-border">
      <div class="text-xs text-text-muted mb-2">{{ stockStore.selectedStock.name }}</div>
      <div class="flex gap-2">
        <button @click="openTradeModal('BUY')" class="flex-1 btn-up text-sm py-2">매수</button>
        <button @click="openTradeModal('SELL')" class="flex-1 btn-down text-sm py-2">매도</button>
      </div>
    </div>

    <!-- Holdings -->
    <div class="flex-1 overflow-y-auto">
      <div class="px-4 py-2 text-xs text-text-muted font-medium border-b border-border">보유 종목</div>
      <div v-if="portfolio?.holdings?.length === 0" class="px-4 py-6 text-center text-text-muted text-sm">
        보유 종목이 없습니다.
      </div>
      <div
        v-for="holding in portfolio?.holdings"
        :key="holding.symbol"
        class="px-4 py-3 border-b border-border hover:bg-bg-secondary cursor-pointer"
        @click="stockStore.selectStock(holding.symbol)"
      >
        <div class="flex justify-between items-start">
          <div>
            <div class="text-sm font-medium">{{ holding.name }}</div>
            <div class="text-xs text-text-muted mt-0.5">{{ holding.quantity }}주 · 평균 {{ formatAmount(holding.avgPrice) }}</div>
          </div>
          <div class="text-right">
            <div class="text-sm font-semibold">{{ formatAmount(holding.currentValue) }}</div>
            <div class="text-xs mt-0.5" :class="holding.profitLoss >= 0 ? 'text-up' : 'text-down'">
              {{ holding.profitLoss >= 0 ? '+' : '' }}{{ formatAmount(holding.profitLoss) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Trade Modal -->
    <TradeModal
      v-if="tradeModal.show"
      :type="tradeModal.type"
      @close="tradeModal.show = false"
      @success="onTradeSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { usePortfolioStore } from '@/stores/portfolio'
import { useStockStore } from '@/stores/stock'
import { formatAmount, formatChangeRate } from '@/utils/format'
import TradeModal from './TradeModal.vue'

const portfolioStore = usePortfolioStore()
const stockStore = useStockStore()

const portfolio = computed(() => portfolioStore.portfolio)
const totalPortfolioValue = computed(() => {
  if (!portfolio.value) return 0
  return Number(portfolio.value.availableCash) + Number(portfolio.value.currentValue)
})

const tradeModal = ref({ show: false, type: 'BUY' })

function openTradeModal(type) {
  tradeModal.value = { show: true, type }
}

async function onTradeSuccess() {
  tradeModal.value.show = false
  await portfolioStore.fetchPortfolio()
}

onMounted(() => {
  portfolioStore.fetchPortfolio()
})
</script>
