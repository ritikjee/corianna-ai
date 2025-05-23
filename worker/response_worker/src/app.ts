import 'dotenv/config'

import { KafkaComnsumer } from './services/kafka/consumer'
import { KafkaProducer } from './services/kafka/producer'
import { RedisClient } from './services/redis'
import { KAFKA_DATA_PAYLOAD } from './types'
import { MessageHandler } from './utils/hande-messages'
import { logger } from './utils/logger'

async function main() {
    const KAFKA_CLIENT_BROKER = process.env.KAFKA_CLIENT_BROKER
    const KAFKA_PRODUCER_BROKER = process.env.KAFKA_PRODUCER_BROKER

    if (!KAFKA_CLIENT_BROKER || !KAFKA_PRODUCER_BROKER) {
        logger.error('Missing environment variables')
        process.exit(1)
    }

    const kafkaConsumer = new KafkaComnsumer(
        [KAFKA_CLIENT_BROKER],
        'response-worker'
    )

    const dbSaveProducer = new KafkaProducer(
        [KAFKA_PRODUCER_BROKER],
        'chat-answers'
    )

    const webhookWorkerProducer = new KafkaProducer(
        [KAFKA_PRODUCER_BROKER],
        'webhook-worker'
    )

    const redis = new RedisClient()

    await Promise.all([
        kafkaConsumer.connect(),
        dbSaveProducer.connect(),
        webhookWorkerProducer.connect(),
        redis.connect(),
    ])

    logger.info('Connected to Kafka and Redis')

    kafkaConsumer.subscribe(async (payload) => {
        const {
            message: { value, offset },
            partition,
            topic,
        } = payload

        kafkaConsumer.commitOffsets([
            {
                topic,
                partition,
                offset: (parseInt(offset, 10) + 1).toString(),
            },
        ])

        if (!value) {
            return
        }

        const data: KAFKA_DATA_PAYLOAD = JSON.parse(value.toString())

        if (!data) {
            logger.error('No data found')
            return
        }

        await Promise.all([
            MessageHandler.handleMessage(redis, data),
            dbSaveProducer.sendMessage(
                JSON.stringify({
                    data: {
                        ...data.response,
                        answer: JSON.stringify(data.response.answer),
                        type: data.metadata.type,
                    },
                    usageMetadata: data.usageMetadata,
                })
            ),
            webhookWorkerProducer.sendMessage(
                JSON.stringify({
                    data: {
                        ...data.response,
                    },
                    answer: JSON.stringify(data.response.answer),
                })
            ),
        ])
    })
}

main()
