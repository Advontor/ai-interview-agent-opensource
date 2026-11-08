import request from '../utils/request'

export interface MediaCapabilities {
  text: boolean
  browserCapture: boolean
  webSocket: boolean
  serverAsr: boolean
  serverTts: boolean
  videoPreview: boolean
  videoUpload: boolean
  notice?: string
}

export const getMediaCapabilities = (): Promise<MediaCapabilities> =>
  request.get('/interview/media/capabilities') as unknown as Promise<MediaCapabilities>

export function mediaWebSocketUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/api/interview/media/ws`
}
