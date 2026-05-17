import { Client } from '@stomp/stompjs'

export function createStompClient() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = import.meta.env.DEV ? 'localhost:8080' : window.location.host

  return new Client({
    brokerURL: `${protocol}//${host}/ws`,
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('WebSocket connected')
    },
    onDisconnect: () => {
      console.log('WebSocket disconnected')
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame)
    }
  })
}
