import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/api'

export const usePortfolioStore = defineStore('portfolio', () => {
  const portfolio = ref(null)
  const isLoading = ref(false)
  const error = ref(null)

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

  async function executeTrade(symbol, quantity, type) {
    const { data } = await api.post('/trade/execute', { symbol, quantity, type })
    await fetchPortfolio()
    return data
  }

  async function getTradeHistory() {
    const { data } = await api.get('/trade/history')
    return data
  }

  return { portfolio, isLoading, error, fetchPortfolio, executeTrade, getTradeHistory }
})
