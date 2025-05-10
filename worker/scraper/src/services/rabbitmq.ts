import * as amqp from 'amqplib'
import { logger } from '../utils/logger'

export class RabbitMQ {
    private connection: amqp.ChannelModel | null = null
    private channel: amqp.Channel | null = null
    private url: string

    constructor(url: string) {
        this.url = url
    }

    async connect() {
        try {
            if (this.connection) {
                return
            }
            this.connection = await amqp.connect(this.url)
            this.channel = await this.connection.createChannel()
        } catch (error) {
            logger.error('Failed to connect to RabbitMQ:', error)
            process.exit(1)
        }
    }

    async consumeMessages(
        queue: string,
        callback: (msg: amqp.ConsumeMessage) => Promise<void>
    ) {
        if (!this.channel) {
            logger.error('RabbitMQ channel is not initialized')
            return
        }

        await this.channel.assertQueue(queue, { durable: false })

        this.channel.consume(queue, async (msg) => {
            if (msg) {
                await callback(msg)
                this.channel!.ack(msg)
            }
        })
    }
}
