import * as amqp from 'amqplib'

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
                console.log('Already connected to RabbitMQ')
                return
            }
            this.connection = await amqp.connect(this.url)
            this.channel = await this.connection.createChannel()
            console.log('Connected to RabbitMQ')
        } catch (error) {
            console.error('Failed to connect to RabbitMQ:', error)
            process.exit(1)
        }
    }

    async consumeMessages(
        queue: string,
        callback: (msg: amqp.ConsumeMessage) => Promise<void>
    ) {
        if (!this.channel) {
            console.error('RabbitMQ channel is not initialized')
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
