<template>
  <div class="border-b border-border bg-bg-primary px-6 overflow-x-auto">
    <div class="flex items-center gap-8 h-11 text-sm whitespace-nowrap">
      <div v-for="index in marketIndices" :key="index.name" class="flex items-center gap-3 shrink-0">
        <span class="text-text-secondary">{{ index.name }}</span>
        <span class="font-semibold">{{ formatIndexValue(index) }}</span>
        <span :class="index.change > 0 ? 'text-up' : index.change < 0 ? 'text-down' : ''" class="text-xs">
          {{ index.change > 0 ? '+' : '' }}{{ index.change.toFixed(2) }}%
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { createStompClient } from '@/utils/websocket'
import api from '@/utils/api'

const marketIndices = ref([
  { name: 'KOSPI',      value: 0, change: 0 },
  { name: 'KOSDAQ',     value: 0, change: 0 },
  { name: 'S&P500',     value: 0, change: 0 },
  { name: 'NASDAQ',     value: 0, change: 0 },
  { name: '나스닥100 선물', value: 0, change: 0 },
  { name: '달러/원',     value: 0, change: 0 },
])

let refreshIntervalId = null
let stompClient = null

function formatIndexValue(index) {
  if (index.value === 0) return '로딩 중...'
  return index.value.toLocaleString('ko-KR', { maximumFractionDigits: 2 })
}

async function fetchIndices() {
  try {
    const { data } = await api.get('/stocks/indices')
    data.forEach(item => {
      const idx = marketIndices.value.findIndex(i => i.name === item.name)
      if (idx !== -1) {
        marketIndices.value[idx] = {
          ...marketIndices.value[idx],
          value: item.value,
          change: item.changeRate,
        }
      }
    })
  } catch (e) {
    // Keep previous values on error
  }
}

onMounted(async () => {
  // Fetch real index data on mount
  await fetchIndices()

  // Refresh indices every 60 seconds
  refreshIntervalId = setInterval(fetchIndices, 60000)

  // Subscribe to real exchange rate via WebSocket
  try {
    stompClient = createStompClient()
    stompClient.onConnect = () => {
      stompClient.subscribe('/topic/exchange-rate', (message) => {
        try {
          const data = JSON.parse(message.body)
          if (data.rate) {
            const idx = marketIndices.value.findIndex(i => i.name === '달러/원')
            if (idx !== -1) {
              const newRate = Number(data.rate)
              const oldRate = marketIndices.value[idx].value
              const change = oldRate > 0 ? +((newRate - oldRate) / oldRate * 100).toFixed(2) : 0
              marketIndices.value[idx] = { ...marketIndices.value[idx], value: newRate, change }
            }
          }
        } catch (_) {}
      })
    }
    stompClient.activate()
  } catch (_) {}
})

onUnmounted(() => {
  if (refreshIntervalId) clearInterval(refreshIntervalId)
  if (stompClient) stompClient.deactivate()
})
</script>
