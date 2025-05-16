import { Schema, model } from 'mongoose'

type WEBHOOK_SCHEMA = {
    appId: string
    webhookId: string
    messageId: string
    chatId: string
    url: string
    name: string
    response: {
        status: number
        success: boolean
        message: string
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        data: any
    }
}

const webhookSchema = new Schema<WEBHOOK_SCHEMA>(
    {
        appId: { type: String, required: true },
        webhookId: { type: String, required: true },
        messageId: { type: String, required: true },
        chatId: { type: String, required: true },
        url: { type: String, required: true },
        name: { type: String, required: true },
        response: {
            status: { type: Number, required: true },
            success: { type: Boolean, required: true },
            message: { type: String, required: true },
            data: { type: Object, required: true },
        },
    },
    {
        timestamps: true,
    }
)

const Webhook = model<WEBHOOK_SCHEMA>('Webhook', webhookSchema)
export default Webhook
