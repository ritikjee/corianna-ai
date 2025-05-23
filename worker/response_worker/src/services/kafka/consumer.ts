import { Consumer, EachMessagePayload, Kafka } from 'kafkajs'
import { logger } from '../../utils/logger'
import { kafkaLogCreator } from './helper'

export class KafkaComnsumer {
    private kafka: Kafka
    private consumer: Consumer
    private topic: string

    constructor(brokers: string[], topic: string) {
        this.kafka = new Kafka({
            clientId: 'chat_worker',
            brokers,
            logCreator: kafkaLogCreator,
        })
        this.consumer = this.kafka.consumer({ groupId: 'chat_worker_group' })
        this.topic = topic
    }

    async connect() {
        try {
            await this.consumer.connect()
        } catch (error) {
            logger.error('Error connecting to Kafka consumer:', error)
            process.exit(1)
        }
    }

    async subscribe(onMessage: (payload: EachMessagePayload) => Promise<void>) {
        try {
            await this.consumer.subscribe({
                topic: this.topic,
                fromBeginning: true,
            })
            logger.info(`Subscribed to Kafka topic: ${this.topic}`)

            await this.consumer.run({
                autoCommit: false,
                eachMessage: async (payload) => {
                    await onMessage(payload)
                },
            })
        } catch (error) {
            logger.error('Error subscribing to Kafka topic:', error)
        }
    }

    async commitOffsets(
        offsets: { topic: string; partition: number; offset: string }[]
    ) {
        try {
            await this.consumer.commitOffsets(offsets)
        } catch (error) {
            logger.error('Error committing offsets:', error)
        }
    }
}
