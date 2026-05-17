<template>
  <span :class="colorClass">
    <span class="font-semibold">{{ displayPrice }}</span>
    <span v-if="showChange" class="ml-1 text-sm">{{ displayChange }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { formatPrice, formatChangeRate } from '@/utils/format'

const props = defineProps({
  price: { type: [Number, String], required: true },
  changeRate: { type: [Number, String], default: 0 },
  market: { type: String, default: 'KOSPI' },
  showChange: { type: Boolean, default: true }
})

const isUp = computed(() => Number(props.changeRate) > 0)
const isDown = computed(() => Number(props.changeRate) < 0)

const colorClass = computed(() => {
  if (isUp.value) return 'text-up'
  if (isDown.value) return 'text-down'
  return 'text-text-primary'
})

const displayPrice = computed(() => formatPrice(props.price, props.market))
const displayChange = computed(() => formatChangeRate(props.changeRate))
</script>
