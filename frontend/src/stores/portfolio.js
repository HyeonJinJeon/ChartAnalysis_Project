import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/api'

export const usePortfolioStore = defineStore('portfolio', () => {
  const portfolio = ref(null)
  const isLoading = ref(false)
  const error = ref(null)
  const exchangeRate = ref(null)

  async function fetchPortfolio() {
    isLoading.value = true
    error.value = null
    try {
      const { data } = await api.get('/portfolio/me')
      portfolio.value = data
    } catch (e) {
      error.value = e.message
    } finally {
      isLoading.value = false
    }
  }

  async function fetchExchangeRate() {
    try {
      const { data } = await api.get('/exchange/rate')
      exchangeRate.value = data
    } catch (e) {
      // ignore — rate widget will show last known value
    }
  }

  async function exchangeCurrency(fromCurrency, amount) {
    const { data } = await api.post('/exchange', { fromCurrency, amount })
    await fetchPortfolio()
    return data
  }

  async function executeTrade(symbol, quantity, type) {
    const { data } = await api.post('/trade/execute', { symbol, quantity, type })
    await fetchPortfolio()
    return data
  }

  async function getTradeHistory() {
    const { data } = await api.get('/trade/history')
    return data
  }

  return {
    portfolio, isLoading, error, exchangeRate,
    fetchPortfolio, fetchExchangeRate, exchangeCurrency,
    executeTrade, getTradeHistory
  }
})
