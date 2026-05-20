<template>
  <div class="relative w-full h-full flex flex-col">
    <!-- Interval selector buttons -->
    <div class="flex items-center gap-1 px-3 py-2 shrink-0">
      <button
        v-for="btn in intervalButtons"
        :key="btn.value"
        @click="changeInterval(btn.value)"
        class="px-3 py-1 text-xs font-medium rounded transition-colors"
        :class="stockStore.currentInterval === btn.value
          ? 'bg-[#2a2a2a] text-white'
          : 'text-[#666] hover:text-[#999]'"
      >
        {{ btn.label }}
      </button>
    </div>

    <!-- Chart area -->
    <div class="relative flex-1">
      <div ref="chartContainer" class="w-full h-full"></div>
      <div v-if="!stockStore.selectedStock" class="absolute inset-0 flex items-center justify-center text-text-muted">
        종목을 선택하세요
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { createChart } from 'lightweight-charts'
import { useStockStore } from '@/stores/stock'

const chartContainer = ref(null)
const stockStore = useStockStore()
let chart = null
let candleSeries = null
let volumeSeries = null

const intervalButtons = [
  { label: '1분', value: '1m' },
  { label: '일봉', value: '1d' },
  { label: '주봉', value: '1wk' },
  { label: '월봉', value: '1mo' },
]

async function changeInterval(interval) {
  await stockStore.setInterval(interval)
}

onMounted(() => {
  chart = createChart(chartContainer.value, {
    layout: {
      background: { color: '#0d0d0d' },
      textColor: '#9e9e9e',
    },
    grid: {
      vertLines: { color: '#1a1a1a' },
      horzLines: { color: '#1a1a1a' },
    },
    crosshair: {
      mode: 1,
    },
    rightPriceScale: {
      borderColor: '#2a2a2a',
    },
    timeScale: {
      borderColor: '#2a2a2a',
      timeVisible: true,
      secondsVisible: false,
    },
    handleScroll: true,
    handleScale: true,
  })

  candleSeries = chart.addCandlestickSeries({
    upColor: '#f04251',
    downColor: '#4b8cf7',
    borderUpColor: '#f04251',
    borderDownColor: '#4b8cf7',
    wickUpColor: '#f04251',
    wickDownColor: '#4b8cf7',
  })

  volumeSeries = chart.addHistogramSeries({
    priceFormat: { type: 'volume' },
    priceScaleId: 'volume',
    color: '#2a2a2a',
  })

  chart.priceScale('volume').applyOptions({
    scaleMargins: { top: 0.8, bottom: 0 },
  })

  const ro = new ResizeObserver(entries => {
    if (entries.length === 0 || !chart) return
    const { width, height } = entries[0].contentRect
    chart.applyOptions({ width, height })
  })
  ro.observe(chartContainer.value)

  if (stockStore.candles.length > 0) {
    updateChart()
  }
})

onUnmounted(() => {
  if (chart) {
    chart.remove()
    chart = null
  }
})

watch(() => stockStore.candles, updateChart, { deep: true })

watch(() => stockStore.selectedStock, (stock) => {
  if (stock && stockStore.candles.length > 0) {
    updateChart()
  }
})

function updateChart() {
  if (!candleSeries || stockStore.candles.length === 0) return

  const candleData = stockStore.candles.map(c => ({
    time: Math.floor(new Date(c.timestamp).getTime() / 1000),
    open: Number(c.open),
    high: Number(c.high),
    low: Number(c.low),
    close: Number(c.close),
  })).sort((a, b) => a.time - b.time)

  const volumeData = stockStore.candles.map(c => ({
    time: Math.floor(new Date(c.timestamp).getTime() / 1000),
    value: Number(c.volume),
    color: Number(c.close) >= Number(c.open) ? '#f0425133' : '#4b8cf733',
  })).sort((a, b) => a.time - b.time)

  candleSeries.setData(candleData)
  volumeSeries.setData(volumeData)
  chart.timeScale().fitContent()
}
</script>
