<template>
  <div class="flex flex-col h-screen bg-[#0d0d0d]">
    <NavBar />
    <div class="mt-14 flex-1 overflow-y-auto">
      <div class="max-w-5xl mx-auto px-6 py-8">
        <h1 class="text-2xl font-bold text-white mb-6">주식 골라보기</h1>

        <!-- Filter tabs -->
        <div class="flex gap-2 mb-6">
          <button
            v-for="tab in tabs"
            :key="tab"
            @click="activeTab = tab"
            class="px-4 py-2 text-sm font-medium rounded-lg transition-colors"
            :class="activeTab === tab
              ? 'bg-[#2a2a2a] text-white'
              : 'text-[#666] hover:text-[#999]'"
          >
            {{ tab }}
          </button>
        </div>

        <!-- Table -->
        <div class="rounded-xl border border-[#1a1a1a] overflow-hidden">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-[#1a1a1a] bg-[#111]">
                <th class="text-left px-4 py-3 text-[#666] font-medium">종목명</th>
                <th class="text-left px-4 py-3 text-[#666] font-medium">심볼</th>
                <th class="text-left px-4 py-3 text-[#666] font-medium">시장</th>
                <th
                  class="text-right px-4 py-3 font-medium cursor-pointer select-none transition-colors"
                  :class="sortKey === 'currentPrice' ? 'text-white' : 'text-[#666] hover:text-[#999]'"
                  @click="setSort('currentPrice')"
                >
                  현재가 {{ sortKey === 'currentPrice' ? (sortDir === 'desc' ? '▼' : '▲') : '' }}
                </th>
                <th
                  class="text-right px-4 py-3 font-medium cursor-pointer select-none transition-colors"
                  :class="sortKey === 'changeRate' ? 'text-white' : 'text-[#666] hover:text-[#999]'"
                  @click="setSort('changeRate')"
                >
                  등락률 {{ sortKey === 'changeRate' ? (sortDir === 'desc' ? '▼' : '▲') : '' }}
                </th>
                <th
                  class="text-right px-4 py-3 font-medium cursor-pointer select-none transition-colors"
                  :class="sortKey === 'volume' ? 'text-white' : 'text-[#666] hover:text-[#999]'"
                  @click="setSort('volume')"
                >
                  거래량 {{ sortKey === 'volume' ? (sortDir === 'desc' ? '▼' : '▲') : '' }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-if="isLoading"
              >
                <td colspan="6" class="text-center py-12 text-[#666]">불러오는 중...</td>
              </tr>
              <tr
                v-else-if="displayedStocks.length === 0"
              >
                <td colspan="6" class="text-center py-12 text-[#666]">종목이 없습니다.</td>
              </tr>
              <tr
                v-for="stock in displayedStocks"
                :key="stock.symbol"
                @click="goToStock(stock)"
                class="border-b border-[#111] hover:bg-[#111] cursor-pointer transition-colors"
              >
                <td class="px-4 py-3 font-medium text-white">{{ stock.name }}</td>
                <td class="px-4 py-3 text-[#888]">{{ stock.symbol }}</td>
                <td class="px-4 py-3">
                  <span
                    class="text-xs px-2 py-0.5 rounded font-medium"
                    :class="marketBadgeClass(stock.market)"
                  >{{ stock.market }}</span>
                </td>
                <td class="px-4 py-3 text-right font-mono text-white">
                  {{ formatPrice(stock.currentPrice, stock.market) }}
                </td>
                <td class="px-4 py-3 text-right font-mono font-medium" :class="changeClass(stock.changeRate)">
                  {{ formatChangeRate(stock.changeRate) }}
                </td>
                <td class="px-4 py-3 text-right text-[#888] font-mono">
                  {{ formatVolume(stock.volume) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '@/components/layout/NavBar.vue'
import { useStockStore } from '@/stores/stock'
import { formatPrice, formatChangeRate, formatVolume } from '@/utils/format'

const router = useRouter()
const stockStore = useStockStore()

const tabs = ['전체', '국내', '해외']
const activeTab = ref('전체')
const sortKey = ref('changeRate')
const sortDir = ref('desc')
const isLoading = ref(false)

onMounted(async () => {
  if (stockStore.stocks.length === 0) {
    isLoading.value = true
    try {
      await stockStore.fetchStocks()
    } finally {
      isLoading.value = false
    }
  }
})

const filteredStocks = computed(() => {
  if (activeTab.value === '국내') {
    return stockStore.stocks.filter(s => s.market === 'KOSPI' || s.market === 'KOSDAQ')
  } else if (activeTab.value === '해외') {
    return stockStore.stocks.filter(s => s.market === 'NASDAQ')
  }
  return stockStore.stocks
})

const displayedStocks = computed(() => {
  const list = [...filteredStocks.value]
  list.sort((a, b) => {
    const av = Number(a[sortKey.value]) || 0
    const bv = Number(b[sortKey.value]) || 0
    return sortDir.value === 'desc' ? bv - av : av - bv
  })
  return list
})

function setSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'desc' ? 'asc' : 'desc'
  } else {
    sortKey.value = key
    sortDir.value = 'desc'
  }
}

async function goToStock(stock) {
  await stockStore.selectStock(stock.symbol)
  router.push('/')
}

function changeClass(rate) {
  const n = Number(rate)
  if (n > 0) return 'text-[#f04251]'
  if (n < 0) return 'text-[#4b8cf7]'
  return 'text-white'
}

function marketBadgeClass(market) {
  if (market === 'NASDAQ') return 'bg-[#1a2a3a] text-[#4b8cf7]'
  if (market === 'KOSPI') return 'bg-[#1a1a2a] text-[#9b8cf7]'
  return 'bg-[#1a2a1a] text-[#4bf78c]'
}
</script>
