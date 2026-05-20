<template>
  <div class="flex flex-col h-screen">
    <NavBar />
    <div class="mt-14 flex-1 overflow-y-auto">
      <div class="max-w-5xl mx-auto px-6 py-8">
        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-2xl font-bold mb-1">내 계좌</h1>
          <p class="text-text-muted text-sm">매월 1일 100만원 시드머니가 지급됩니다</p>
        </div>

        <LoadingSpinner v-if="portfolioStore.isLoading" />

        <template v-else-if="portfolio">
          <!-- Summary cards -->
          <div class="grid grid-cols-3 gap-4 mb-8">
            <div class="card p-5">
              <div class="text-sm text-text-muted mb-1">총 자산</div>
              <div class="text-2xl font-bold">{{ formatAmount(totalValue) }}</div>
            </div>
            <div class="card p-5">
              <div class="text-sm text-text-muted mb-1">총 손익</div>
              <div class="text-2xl font-bold" :class="portfolio.totalProfitLoss >= 0 ? 'text-up' : 'text-down'">
                {{ portfolio.totalProfitLoss >= 0 ? '+' : '' }}{{ formatAmount(portfolio.totalProfitLoss) }}
              </div>
              <div class="text-sm" :class="portfolio.totalProfitLoss >= 0 ? 'text-up' : 'text-down'">
                {{ formatChangeRate(portfolio.totalProfitLossRate) }}
              </div>
            </div>
            <div class="card p-5">
              <div class="text-sm text-text-muted mb-1">주문 가능 금액</div>
              <div class="text-2xl font-bold">{{ formatAmount(portfolio.availableCash) }}</div>
            </div>
          </div>

          <!-- Holdings table -->
          <div class="card mb-8">
            <div class="px-6 py-4 border-b border-border font-semibold">보유 종목</div>
            <div v-if="portfolio.holdings.length === 0" class="px-6 py-8 text-center text-text-muted">
              보유 종목이 없습니다.
            </div>
            <table v-else class="w-full">
              <thead>
                <tr class="text-xs text-text-muted border-b border-border">
                  <th class="px-6 py-3 text-left font-medium">종목</th>
                  <th class="px-6 py-3 text-right font-medium">보유수량</th>
                  <th class="px-6 py-3 text-right font-medium">평균단가</th>
                  <th class="px-6 py-3 text-right font-medium">현재가</th>
                  <th class="px-6 py-3 text-right font-medium">평가금액</th>
                  <th class="px-6 py-3 text-right font-medium">손익</th>
                  <th class="px-6 py-3 text-right font-medium">수익률</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="holding in portfolio.holdings"
                  :key="holding.symbol"
                  class="border-b border-border hover:bg-bg-secondary transition-colors"
                >
                  <td class="px-6 py-4">
                    <div class="font-semibold">{{ holding.name }}</div>
                    <div class="text-xs text-text-muted">{{ holding.symbol }}</div>
                  </td>
                  <td class="px-6 py-4 text-right text-sm">{{ holding.quantity }}주</td>
                  <td class="px-6 py-4 text-right text-sm">{{ formatAmountByCurrency(holding.avgPrice, holding.market) }}</td>
                  <td class="px-6 py-4 text-right text-sm">{{ formatAmountByCurrency(holding.currentPrice, holding.market) }}</td>
                  <td class="px-6 py-4 text-right text-sm font-semibold">{{ formatAmountByCurrency(holding.currentValue, holding.market) }}</td>
                  <td class="px-6 py-4 text-right text-sm font-semibold" :class="holding.profitLoss >= 0 ? 'text-up' : 'text-down'">
                    {{ holding.profitLoss >= 0 ? '+' : '' }}{{ formatAmountByCurrency(holding.profitLoss, holding.market) }}
                  </td>
                  <td class="px-6 py-4 text-right text-sm font-semibold" :class="holding.profitLossRate >= 0 ? 'text-up' : 'text-down'">
                    {{ formatChangeRate(holding.profitLossRate) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Trade history -->
          <div class="card">
            <div class="px-6 py-4 border-b border-border font-semibold">거래 내역</div>
            <div v-if="tradeHistory.length === 0" class="px-6 py-8 text-center text-text-muted">
              거래 내역이 없습니다.
            </div>
            <table v-else class="w-full">
              <thead>
                <tr class="text-xs text-text-muted border-b border-border">
                  <th class="px-6 py-3 text-left font-medium">일시</th>
                  <th class="px-6 py-3 text-left font-medium">종목</th>
                  <th class="px-6 py-3 text-center font-medium">구분</th>
                  <th class="px-6 py-3 text-right font-medium">수량</th>
                  <th class="px-6 py-3 text-right font-medium">단가</th>
                  <th class="px-6 py-3 text-right font-medium">총금액</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="trade in tradeHistory"
                  :key="trade.tradeId"
                  class="border-b border-border hover:bg-bg-secondary transition-colors"
                >
                  <td class="px-6 py-3 text-xs text-text-muted">{{ formatDate(trade.tradedAt) }}</td>
                  <td class="px-6 py-3">
                    <div class="text-sm font-medium">{{ trade.stockName }}</div>
                    <div class="text-xs text-text-muted">{{ trade.symbol }}</div>
                  </td>
                  <td class="px-6 py-3 text-center">
                    <span
                      class="text-xs font-bold px-2 py-1 rounded"
                      :class="trade.type === 'BUY' ? 'bg-up/20 text-up' : 'bg-down/20 text-down'"
                    >
                      {{ trade.type === 'BUY' ? '매수' : '매도' }}
                    </span>
                  </td>
                  <td class="px-6 py-3 text-right text-sm">{{ trade.quantity }}주</td>
                  <td class="px-6 py-3 text-right text-sm">{{ formatAmountByCurrency(trade.price, trade.market) }}</td>
                  <td class="px-6 py-3 text-right text-sm font-semibold">{{ formatAmountByCurrency(trade.totalAmount, trade.market) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import NavBar from '@/components/layout/NavBar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import { usePortfolioStore } from '@/stores/portfolio'
import { formatAmount, formatAmountByCurrency, formatChangeRate } from '@/utils/format'

const portfolioStore = usePortfolioStore()
const portfolio = computed(() => portfolioStore.portfolio)
const tradeHistory = ref([])

const totalValue = computed(() => {
  if (!portfolio.value) return 0
  return Number(portfolio.value.availableCash) + Number(portfolio.value.currentValue)
})

function formatDate(dateStr) {
  return new Date(dateStr).toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(async () => {
  await portfolioStore.fetchPortfolio()
  tradeHistory.value = await portfolioStore.getTradeHistory()
})
</script>
