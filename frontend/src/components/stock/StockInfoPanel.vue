<template>
  <div class="flex flex-col h-full bg-[#0d0d0d] border-t border-[#1a1a1a]">
    <!-- Tab bar -->
    <div class="flex shrink-0 border-b border-[#1a1a1a]">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        @click="activeTab = tab.key"
        class="px-4 py-2 text-xs font-medium transition-colors border-b-2"
        :class="activeTab === tab.key
          ? 'border-[#4b8cf7] text-white'
          : 'border-transparent text-[#666] hover:text-[#999]'"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 기업정보 tab -->
    <div v-if="activeTab === 'info'" class="flex-1 overflow-y-auto px-4 py-3">
      <div v-if="stockInfo" class="space-y-3">
        <!-- Price row -->
        <div class="flex items-baseline gap-3">
          <span class="text-2xl font-bold" :class="changeClass(stockInfo.changeRate)">
            {{ formatPrice(stockInfo.currentPrice, stockInfo.market) }}
          </span>
          <span class="text-sm font-medium" :class="changeClass(stockInfo.changeRate)">
            {{ formatChangeRate(stockInfo.changeRate) }}
          </span>
          <span
            class="ml-auto text-xs px-2 py-0.5 rounded font-medium"
            :class="marketBadgeClass(stockInfo.market)"
          >{{ stockInfo.market }}</span>
        </div>

        <!-- Info grid -->
        <div class="grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
          <div class="flex justify-between">
            <span class="text-[#666]">전일종가</span>
            <span class="text-white font-mono">{{ formatPrice(stockInfo.previousClose, stockInfo.market) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-[#666]">거래량</span>
            <span class="text-white font-mono">{{ formatVolume(stockInfo.volume) }}</span>
          </div>
          <div v-if="stockInfo.weekHigh52" class="flex justify-between">
            <span class="text-[#666]">52주 고가</span>
            <span class="text-[#f04251] font-mono">{{ formatPrice(stockInfo.weekHigh52, stockInfo.market) }}</span>
          </div>
          <div v-if="stockInfo.weekLow52" class="flex justify-between">
            <span class="text-[#666]">52주 저가</span>
            <span class="text-[#4b8cf7] font-mono">{{ formatPrice(stockInfo.weekLow52, stockInfo.market) }}</span>
          </div>
        </div>
      </div>

      <!-- Skeleton loading -->
      <div v-else-if="infoLoading" class="space-y-3">
        <div class="h-7 w-40 bg-[#1a1a1a] rounded animate-pulse"></div>
        <div class="grid grid-cols-2 gap-2">
          <div v-for="i in 4" :key="i" class="h-4 bg-[#1a1a1a] rounded animate-pulse"></div>
        </div>
      </div>

      <div v-else class="text-[#666] text-xs py-4 text-center">정보를 불러올 수 없습니다</div>
    </div>

    <!-- 관련뉴스 tab -->
    <div v-if="activeTab === 'news'" class="flex-1 overflow-y-auto">
      <!-- Skeleton -->
      <div v-if="newsLoading" class="px-4 py-3 space-y-3">
        <div v-for="i in 5" :key="i" class="space-y-1.5">
          <div class="h-4 bg-[#1a1a1a] rounded animate-pulse"></div>
          <div class="h-3 w-3/4 bg-[#1a1a1a] rounded animate-pulse"></div>
          <div class="h-3 w-1/2 bg-[#1a1a1a] rounded animate-pulse"></div>
        </div>
      </div>

      <!-- News list -->
      <div v-else-if="newsList.length > 0">
        <a
          v-for="(item, idx) in newsList"
          :key="idx"
          :href="item.url"
          target="_blank"
          rel="noopener noreferrer"
          class="block px-4 py-3 border-b border-[#111] hover:bg-[#111] transition-colors cursor-pointer"
        >
          <div class="text-xs font-semibold text-white leading-snug line-clamp-2 mb-1">{{ item.headline }}</div>
          <div class="text-[10px] text-[#666] mb-1">{{ item.source }} · {{ item.publishedAt }}</div>
          <div class="text-[11px] text-[#888] line-clamp-2 leading-relaxed">{{ item.summary }}</div>
        </a>
      </div>

      <!-- No news -->
      <div v-else class="text-[#666] text-xs py-8 text-center">뉴스를 불러올 수 없습니다</div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '@/utils/api'
import { formatPrice, formatChangeRate, formatVolume } from '@/utils/format'

const props = defineProps({
  symbol: {
    type: String,
    default: null
  }
})

const tabs = [
  { key: 'info', label: '기업정보' },
  { key: 'news', label: '관련뉴스' },
]
const activeTab = ref('info')

const stockInfo = ref(null)
const infoLoading = ref(false)
const newsList = ref([])
const newsLoading = ref(false)

async function fetchInfo(symbol) {
  if (!symbol) return
  infoLoading.value = true
  stockInfo.value = null
  try {
    const { data } = await api.get(`/stocks/${symbol}/info`)
    stockInfo.value = data
  } catch {
    stockInfo.value = null
  } finally {
    infoLoading.value = false
  }
}

async function fetchNews(symbol) {
  if (!symbol) return
  newsLoading.value = true
  newsList.value = []
  try {
    const { data } = await api.get(`/stocks/${symbol}/news`)
    newsList.value = data
  } catch {
    newsList.value = []
  } finally {
    newsLoading.value = false
  }
}

watch(() => props.symbol, (sym) => {
  fetchInfo(sym)
  fetchNews(sym)
}, { immediate: true })

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
