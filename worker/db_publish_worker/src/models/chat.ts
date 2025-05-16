import { Schema, model } from 'mongoose'

type CHAT_SCHEMA = {
    answer: string
    question: string
    appId: string
    messageId: string
    chatId: string
    candidatesTokenCount: number
    promptTokenCount: number
    totalTokenCount: number
}
const chatSchema = new Schema<CHAT_SCHEMA>(
    {
        answer: { type: String, required: true },
        question: { type: String, required: true },
        appId: { type: String, required: true },
        messageId: { type: String, required: true },
        chatId: { type: String, required: true },
        candidatesTokenCount: { type: Number, required: true },
        promptTokenCount: { type: Number, required: true },
        totalTokenCount: { type: Number, required: true },
    },
    {
        timestamps: true,
    }
)

const Chat = model<CHAT_SCHEMA>('Chat', chatSchema)
export default Chat
