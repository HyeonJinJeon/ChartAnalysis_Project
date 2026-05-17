import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/utils/api'
import { createStompClient } from '@/utils/websocket'

export const useStockStore = defineStore('stock', () => {
  const stocks = ref([])
  const selectedStock = ref(null)
  const candles = ref([])
  const activeFilter = ref('전체')
  const isLoading = ref(false)
  let stompClient = null
  let currentSubscription = null

  const filteredStocks = computed(() => {
    if (activeFilter.value === '국내') {
      return stocks.value.filter(s => s.market === 'KOSPI' || s.market === 'KOSDAQ')
    } else if (activeFilter.value === '해외') {
      return stocks.value.filter(s => s.market === 'NASDAQ')
    }
    return stocks.value
  })

  async function fetchStocks() {
    const { data } = await api.get('/stocks')
    stocks.value = data
  }

  async function selectStock(symbol) {
    const stock = stocks.value.find(s => s.symbol === symbol)
    selectedStock.value = stock || null
    await fetchCandles(symbol)
    subscribeToStock(symbol)
  }

  async function fetchCandles(symbol, interval = '1m', limit = 100) {
    isLoading.value = true
    try {
      const { data } = await api.get(`/stocks/${symbol}/candles`, {
        params: { interval, limit }
      })
      candles.value = data
    } finally {
      isLoading.value = false
    }
  }

  function connectWebSocket() {
    stompClient = createStompClient()
    stompClient.activate()
  }

  function subscribeToStock(symbol) {
    if (!stompClient || !stompClient.connected) return

    if (currentSubscription) {
      currentSubscription.unsubscribe()
    }

    currentSubscription = stompClient.subscribe(`/topic/stocks/${symbol}`, (message) => {
      const data = JSON.parse(message.body)
      const stockIndex = stocks.value.findIndex(s => s.symbol === data.symbol)
      if (stockIndex !== -1) {
        stocks.value[stockIndex] = { ...stocks.value[stockIndex], ...data }
      }
      if (selectedStock.value?.symbol === data.symbol) {
        selectedStock.value = { ...selectedStock.value, ...data }
      }
    })
  }

  function disconnectWebSocket() {
    if (stompClient) {
      stompClient.deactivate()
    }
  }

  function setFilter(filter) {
    activeFilter.value = filter
  }

  return {
    stocks, selectedStock, candles, activeFilter, isLoading,
    filteredStocks,
    fetchStocks, selectStock, fetchCandles,
    connectWebSocket, disconnectWebSocket, subscribeToStock, setFilter
  }
})
