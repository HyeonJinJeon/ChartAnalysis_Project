import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function createStompClient() {
  return new Client({
    webSocketFactory: () => new SockJS('/ws'),
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
