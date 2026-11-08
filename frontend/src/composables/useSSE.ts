import { ref, onUnmounted } from 'vue';
import type { SSEMessage } from '../types/interview';

export function useSSE() {
  const isConnected = ref(false);
  const processedSequences = ref<Set<number>>(new Set()); // 用于前端去重
  let mockTimer: any = null;

  // 8 题 Mock 题库
  const mockQuestions = [
    '请详细介绍一下你在校园二手交易平台项目中承担的具体工作，以及你遇到的最大技术挑战是什么？',
    'Spring Boot 自动装配的原理是什么？它是如何通过注解实现"约定优于配置"的？',
    '解释一下 JVM 的内存结构，并说明堆和栈的区别。',
    '你在项目中是如何设计数据库表结构的？有没有做过索引优化？',
    '你遇到过最困难的技术问题是什么？你是如何排查并解决的？',
    '什么是 RESTful API？设计时需要注意哪些规范？',
    '谈谈你对 Vue3 响应式原理的理解，它和 Vue2 有什么区别？',
    '如果让你设计一个高并发的秒杀系统，你会从哪些方面考虑？'
  ];
  let currentQuestionIndex = 0;

  type EventHandler = (msg: SSEMessage) => void;
  let onEvent: EventHandler;

  const setEventHandler = (handler: EventHandler) => {
    onEvent = handler;
  };

  // 🌟 模拟 SSE 流
  const connectMockSSE = (eventType: 'QUESTION_STARTED' | 'FOLLOW_UP' | 'SWITCH_TOPIC' = 'QUESTION_STARTED') => {
    if (mockTimer) clearInterval(mockTimer);
    isConnected.value = true;

    const currentText = mockQuestions[currentQuestionIndex % mockQuestions.length];
    currentQuestionIndex++;

    let sequence = 0;
    let charIndex = 0;

    // 1. 追问 / 换题前缀（占一个 sequence）
    if (eventType !== 'QUESTION_STARTED') {
      onEvent({ event: eventType, sequence: sequence++, data: {} });
    }

    // 2. QUESTION_STARTED（占一个 sequence）
    onEvent({ event: 'QUESTION_STARTED', sequence: sequence++, data: {} });

    // 3. 逐字发送 QUESTION_CHUNK（从 sequence=2 开始，如果前面有前缀则更高）
    mockTimer = setInterval(() => {
      if (charIndex < currentText.length) {
        const chunkSize = Math.floor(Math.random() * 3) + 1;
        const content = currentText.slice(charIndex, charIndex + chunkSize);
        charIndex += chunkSize;

        onEvent({
          event: 'QUESTION_CHUNK',
          sequence: sequence++,
          data: { content }
        });
      } else {
        clearInterval(mockTimer);
        onEvent({ event: 'QUESTION_COMPLETED', sequence: sequence++, data: {} });
      }
    }, 100);
  };

  // 🔥 模拟断线重连：故意重发已发过的 sequence，测试前端去重
  const simulateReconnect = () => {
    if (mockTimer) clearInterval(mockTimer);
    isConnected.value = false;
    console.warn('⚡ 模拟断线...');

    setTimeout(() => {
      isConnected.value = true;
      console.log('🔄 正在重连... 模拟后端重发重复的 sequence');

      // ⚠️ 重发已作为 QUESTION_CHUNK 处理过的 sequence（2 和 3），确保被去重
      onEvent({ event: 'QUESTION_CHUNK', sequence: 2, data: { content: '【这行是重连后重复推送的，应该被忽略】' } });
      onEvent({ event: 'QUESTION_CHUNK', sequence: 3, data: { content: '【这行也是重复的，也应该被忽略】' } });

      // 继续发送新内容（用新的 sequence 号，模拟后端发送尚未送达的内容）
      const currentText = mockQuestions[(currentQuestionIndex - 1 + mockQuestions.length) % mockQuestions.length];
      const remainText = currentText.slice(-10);
      let seq = 10000;
      for (let i = 0; i < remainText.length; i++) {
        onEvent({
          event: 'QUESTION_CHUNK',
          sequence: seq++,
          data: { content: remainText[i] }
        });
      }
      onEvent({ event: 'QUESTION_COMPLETED', sequence: seq, data: {} });
    }, 1500);
  };

  const disconnect = () => {
    if (mockTimer) clearInterval(mockTimer);
    isConnected.value = false;
  };

  onUnmounted(() => {
    disconnect();
  });

  return {
    isConnected,
    processedSequences,
    setEventHandler,
    connectMockSSE,
    simulateReconnect,
    disconnect
  };
}