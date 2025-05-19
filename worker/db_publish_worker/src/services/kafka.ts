import { Consumer, Kafka as kafkajs, EachMessagePayload } from 'kafkajs'
import { logger } from '../utils/logger'
import { kafkaLogCreator } from '../utils/kafka-logger'

export class KafkaClient {
    private kafka: kafkajs
    private consumer: Consumer

    constructor(brokers: string[]) {
        this.kafka = new kafkajs({
            clientId: 'my-app',
            brokers,
            logCreator: kafkaLogCreator,
        })
        this.consumer = this.kafka.consumer({
            groupId: 'web-scrapper-embeddings',
        })
    }

    async connect() {
        try {
            await this.consumer.connect()
        } catch (error) {
            logger.error('Error connecting to Kafka:', error)
            process.exit(1)
        }
    }

    async subscribe(
        topics: string[],
        onMessage: (payload: EachMessagePayload) => Promise<void>
    ) {
        for (const topic of topics) {
            await this.consumer.subscribe({
                topic,
                fromBeginning: true,
            })
        }

        logger.info(`Subscribed to topic ${topics.join(', ')}`)

        await this.consumer.run({
            autoCommit: true,
            eachMessage: async (payload) => {
                await onMessage(payload)
            },
        })
    }
}
