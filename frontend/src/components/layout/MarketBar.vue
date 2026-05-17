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

const marketIndices = ref([
  { name: 'KOSPI', value: 2487.12, change: -0.43 },
  { name: 'KOSDAQ', value: 832.56, change: +1.12 },
  { name: 'S&P500', value: 5234.18, change: +0.28 },
  { name: 'NASDAQ', value: 16421.34, change: -0.67 },
  { name: '나스닥100 선물', value: 18204.50, change: -0.31 },
  { name: '달러/원', value: 1380.50, change: +0.15 },
])

let intervalId = null

onMounted(() => {
  intervalId = setInterval(() => {
    marketIndices.value = marketIndices.value.map(index => ({
      ...index,
      value: +(index.value * (1 + (Math.random() - 0.5) * 0.001)).toFixed(2),
      change: +(index.change + (Math.random() - 0.5) * 0.1).toFixed(2)
    }))
  }, 3000)
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})
</script>
