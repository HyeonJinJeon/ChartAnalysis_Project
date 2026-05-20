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
  const currentInterval = ref('1m')
  let stompClient = null
  const stockSubscriptions = new Map()

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
    await fetchCandles(symbol, currentInterval.value)
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

  async function setInterval(interval) {
    currentInterval.value = interval
    if (selectedStock.value) {
      await fetchCandles(selectedStock.value.symbol, interval)
    }
  }

  function connectWebSocket() {
    if (stompClient && stompClient.active) return // already connecting/connected
    stompClient = createStompClient()

    stompClient.onConnect = () => {
      // Subscribe to all currently loaded stocks (may be empty if stocks aren't fetched yet)
      stocks.value.forEach(stock => {
        _subscribeToSymbol(stock.symbol)
      })
    }

    stompClient.activate()
  }

  function _subscribeToSymbol(symbol) {
    if (!stompClient || !stompClient.connected) return
    if (stockSubscriptions.has(symbol)) return // already subscribed

    const sub = stompClient.subscribe(`/topic/stocks/${symbol}`, (message) => {
      try {
        const data = JSON.parse(message.body)

        // Update stocks list reactively using spread to trigger Vue reactivity
        const stockIndex = stocks.value.findIndex(s => s.symbol === data.symbol)
        if (stockIndex !== -1) {
          stocks.value = [
            ...stocks.value.slice(0, stockIndex),
            {
              ...stocks.value[stockIndex],
              currentPrice: data.price,
              changeRate: data.changeRate,
              volume: data.volume,
            },
            ...stocks.value.slice(stockIndex + 1),
          ]
        }

        // Update selectedStock if it matches
        if (selectedStock.value?.symbol === data.symbol) {
          selectedStock.value = {
            ...selectedStock.value,
            currentPrice: data.price,
            changeRate: data.changeRate,
            volume: data.volume,
          }

          // Append new candle to chart if on 1m interval
          if (currentInterval.value === '1m' && data.timestamp) {
            const newCandle = {
              timestamp: data.timestamp,
              open: data.price,
              high: data.price,
              low: data.price,
              close: data.price,
              volume: data.volume,
            }
            // Update last candle or add new one
            const lastCandle = candles.value[candles.value.length - 1]
            if (lastCandle && lastCandle.timestamp === data.timestamp) {
              candles.value = [
                ...candles.value.slice(0, -1),
                {
                  ...lastCandle,
                  close: data.price,
                  high: Math.max(Number(lastCandle.high), Number(data.price)),
                  low: Math.min(Number(lastCandle.low), Number(data.price)),
                  volume: data.volume,
                },
              ]
            } else {
              candles.value = [...candles.value, newCandle]
            }
          }
        }
      } catch (e) {
        console.error('WS message parse error', e)
      }
    })

    stockSubscriptions.set(symbol, sub)
  }

  function subscribeToAllStocks() {
    if (!stompClient) {
      // WebSocket not even initialised yet — connect now
      connectWebSocket()
      return
    }
    if (!stompClient.connected) {
      // Connected event not yet fired; onConnect will call _subscribeToSymbol for each stock
      // Re-register onConnect to also pick up newly loaded stocks
      const prevOnConnect = stompClient.onConnect
      stompClient.onConnect = (frame) => {
        if (prevOnConnect) prevOnConnect(frame)
        stocks.value.forEach(stock => _subscribeToSymbol(stock.symbol))
      }
      return
    }
    stocks.value.forEach(stock => {
      _subscribeToSymbol(stock.symbol)
    })
  }

  function disconnectWebSocket() {
    if (stompClient) {
      stompClient.deactivate()
    }
    stockSubscriptions.clear()
  }

  function setFilter(filter) {
    activeFilter.value = filter
  }

  return {
    stocks, selectedStock, candles, activeFilter, isLoading, currentInterval,
    filteredStocks,
    fetchStocks, selectStock, fetchCandles, setInterval,
    connectWebSocket, disconnectWebSocket, subscribeToAllStocks, setFilter
  }
})
