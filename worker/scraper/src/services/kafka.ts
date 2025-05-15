import { Admin, Kafka as KafkaJS, Producer } from 'kafkajs'
import { KAFKA_DEFAULT_TOPIC } from '../constants'
import { KafkaMessage } from '../types'
import { logger } from '../utils/logger'

export class Kafka {
    private kafka: KafkaJS
    private producer: Producer
    private admin: Admin

    private MAX_RETRIES = 3
    private INITIAL_BACKOFF_MS = 500
    private BATCH_SIZE = 10

    constructor(brokers: string[]) {
        this.kafka = new KafkaJS({
            clientId: 'my-app',
            brokers,
        })
        this.producer = this.kafka.producer()
        this.admin = this.kafka.admin()
    }

    async init() {
        await this.admin.connect()
        const topics = await this.admin.listTopics()
        if (!topics.includes(KAFKA_DEFAULT_TOPIC)) {
            await this.admin.createTopics({
                topics: [{ topic: KAFKA_DEFAULT_TOPIC, numPartitions: 1 }],
            })
        }
        logger.info(`Kafka topics: ${topics.join(', ')}`)
        await this.admin.disconnect()
    }

    async connect() {
        await this.producer.connect()
    }

    async disconnect() {
        await this.producer.disconnect()
    }

    async sendMessage(message: string, topic: string) {
        await this.producer.send({
            topic,
            messages: [{ value: message }],
        })
    }

    async sendMessagesInBatches(messages: KafkaMessage[]): Promise<void> {
        const sendBatch = async (batch: string[]) => {
            for (let attempt = 1; attempt <= this.MAX_RETRIES; attempt++) {
                try {
                    // Adjust this according to your Kafka client's batch API
                    await Promise.all(
                        batch.map((msg) =>
                            this.producer.send({
                                topic: KAFKA_DEFAULT_TOPIC,
                                messages: [{ value: msg }],
                            })
                        )
                    )
                    return // success
                } catch (error) {
                    logger.error(
                        `Kafka batch send failed (attempt ${attempt}):`,
                        error
                    )

                    if (attempt < this.MAX_RETRIES) {
                        const backoff =
                            this.INITIAL_BACKOFF_MS * 2 ** (attempt - 1)
                        await new Promise((resolve) =>
                            setTimeout(resolve, backoff)
                        )
                    } else {
                        throw new Error(
                            `Failed to send Kafka batch after ${this.MAX_RETRIES} retries`
                        )
                    }
                }
            }
        }

        for (let i = 0; i < messages.length; i += this.BATCH_SIZE) {
            const batch = messages.slice(i, i + this.BATCH_SIZE)
            await sendBatch(batch.map((msg) => JSON.stringify(msg)))
        }
    }
}
