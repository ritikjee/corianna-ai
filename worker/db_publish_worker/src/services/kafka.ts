import { Consumer, Kafka as kafkajs, EachMessagePayload } from 'kafkajs'
import { KAFKA_DEFAULT_TOPIC } from '../constants'
import { logger } from '../utils/logger'

export class KafkaClient {
    private kafka: kafkajs
    private consumer: Consumer

    constructor(brokers: string[]) {
        this.kafka = new kafkajs({
            clientId: 'my-app',
            brokers,
        })
        this.consumer = this.kafka.consumer({ groupId: KAFKA_DEFAULT_TOPIC })
    }

    async connect() {
        try {
            await this.consumer.connect()
        } catch (error) {
            logger.error('Error connecting to Kafka:', error)
            process.exit(1)
        }
    }

    async init(onMessage: (payload: EachMessagePayload) => Promise<void>) {
        await this.consumer.subscribe({
            topic: KAFKA_DEFAULT_TOPIC,
            fromBeginning: true,
        })

        logger.info(`Subscribed to topic ${KAFKA_DEFAULT_TOPIC}`)

        await this.consumer.run({
            eachMessage: async (payload) => {
                await onMessage(payload)
            },
        })
    }
}
