<template>
  <div class="border-b border-border bg-bg-primary px-6 overflow-x-auto">
    <div class="flex items-center gap-8 h-11 text-sm whitespace-nowrap">
      <div v-for="index in marketIndices" :key="index.name" class="flex items-center gap-3 shrink-0">
        <span class="text-text-secondary">{{ index.name }}</span>
        <span class="font-semibold">{{ index.value.toLocaleString() }}</span>
        <span :class="index.change > 0 ? 'text-up' : 'text-down'" class="text-xs">
          {{ index.change > 0 ? '+' : '' }}{{ index.change.toFixed(2) }}%
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { createStompClient } from '@/utils/websocket'

const marketIndices = ref([
  { name: 'KOSPI', value: 2487.12, change: -0.43 },
  { name: 'KOSDAQ', value: 832.56, change: +1.12 },
  { name: 'S&P500', value: 5234.18, change: +0.28 },
  { name: 'NASDAQ', value: 16421.34, change: -0.67 },
  { name: '나스닥100 선물', value: 18204.50, change: -0.31 },
  { name: '달러/원', value: 1380.50, change: +0.15 },
])

let intervalId = null
let stompClient = null
let exchangeSubscription = null

onMounted(() => {
  // Simulate other indices
  intervalId = setInterval(() => {
    marketIndices.value = marketIndices.value.map((index, i) => {
      // Don't randomly update '달러/원' — it comes from WebSocket
      if (index.name === '달러/원') return index
      return {
        ...index,
        value: +(index.value * (1 + (Math.random() - 0.5) * 0.001)).toFixed(2),
        change: +(index.change + (Math.random() - 0.5) * 0.1).toFixed(2)
      }
    })
  }, 3000)

  // Subscribe to real exchange rate via WebSocket
  try {
    stompClient = createStompClient()
    stompClient.onConnect = () => {
      exchangeSubscription = stompClient.subscribe('/topic/exchange-rate', (message) => {
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
  if (intervalId) clearInterval(intervalId)
  if (stompClient) stompClient.deactivate()
})
</script>
