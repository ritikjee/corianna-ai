import { Admin, Kafka, Producer } from 'kafkajs'
import { logger } from '../../utils/logger'
import { kafkaLogCreator } from './helper'

export class KafkaProducer {
    private kafka: Kafka
    private producer: Producer
    private admin: Admin
    private topic: string

    constructor(brokers: string[], topic: string) {
        this.kafka = new Kafka({
            clientId: 'chat_worker',
            brokers,
            logCreator: kafkaLogCreator,
        })
        this.producer = this.kafka.producer()
        this.admin = this.kafka.admin()
        this.topic = topic
    }

    async init() {
        await this.admin.connect()
        const topics = await this.admin.listTopics()
        if (!topics.includes(this.topic)) {
            await this.admin.createTopics({
                topics: [{ topic: this.topic, numPartitions: 1 }],
            })
        }
        logger.info(`Kafka topic ${this.topic} is ready`)
        await this.admin.disconnect()
    }

    async connect() {
        try {
            await this.producer.connect()
        } catch (error) {
            logger.error('Error connecting to Kafka producer:', error)
            process.exit(1)
        }
    }

    async sendMessage(message: string) {
        try {
            await this.producer.send({
                topic: this.topic,
                messages: [{ value: message }],
            })
        } catch (error) {
            logger.error('Error sending message to Kafka producer:', error)
        }
    }
}
