export function formatPrice(price, market) {
  if (!price) return '-'
  if (market === 'NASDAQ') {
    return `$${Number(price).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
  }
  return `${Number(price).toLocaleString('ko-KR')}원`
}

export function formatChangeRate(rate) {
  if (rate === null || rate === undefined) return '0.00%'
  const num = Number(rate)
  const sign = num > 0 ? '+' : ''
  return `${sign}${num.toFixed(2)}%`
}

export function formatVolume(volume) {
  if (!volume) return '-'
  if (volume >= 100000000) return `${(volume / 100000000).toFixed(1)}억`
  if (volume >= 10000000) return `${(volume / 10000000).toFixed(1)}천만`
  if (volume >= 1000000) return `${(volume / 1000000).toFixed(1)}백만`
  if (volume >= 10000) return `${(volume / 10000).toFixed(0)}만`
  return volume.toLocaleString()
}

export function formatAmount(amount) {
  if (!amount) return '0원'
  return `${Number(amount).toLocaleString('ko-KR')}원`
}

export function formatAmountByCurrency(amount, market) {
  if (amount === null || amount === undefined) return market === 'NASDAQ' ? '$0.00' : '0원'
  if (market === 'NASDAQ') {
    return `$${Number(amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
  }
  return `${Number(amount).toLocaleString('ko-KR')}원`
}
