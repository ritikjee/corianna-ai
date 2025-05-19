import 'dotenv/config'

import {
    GEMINI_TEXT_EMBEDDING_004_RATE_LIMIT_PER_MINUTE,
    KAFKA_CHAT_EMBEDDINGS_TOPIC,
    KAFKA_CHAT_QUESTIONS_TOPIC,
    REDIS_RATE_LIMIT_KEY,
} from './constants'
import { googleGenAI } from './services/gemini'
import { KafkaComnsumer } from './services/kafka/consumer'
import { KafkaProducer } from './services/kafka/producer'
import { RedisClient } from './services/redis'
import { startRateLimitRefresher } from './utils/helper'
import { logger } from './utils/logger'

async function main() {
    const KAFKA_CLIENT_BROKER = process.env.KAFKA_CLIENT_BROKER
    const KAFKA_PRODUCER_BROKER = process.env.KAFKA_PRODUCER_BROKER
    const REDIS_CONNECTION_URI = process.env.REDIS_CONNECTION_URI

    if (
        !KAFKA_CLIENT_BROKER ||
        !KAFKA_PRODUCER_BROKER ||
        !REDIS_CONNECTION_URI
    ) {
        logger.error('Missing environment variables')
        process.exit(1)
    }

    const kafkaConsumer = new KafkaComnsumer(
        [KAFKA_CLIENT_BROKER],
        KAFKA_CHAT_QUESTIONS_TOPIC
    )

    const kafkaProducer = new KafkaProducer(
        [KAFKA_PRODUCER_BROKER],
        KAFKA_CHAT_EMBEDDINGS_TOPIC
    )

    const redisClient = new RedisClient(REDIS_CONNECTION_URI)

    await Promise.all([
        kafkaConsumer.connect(),
        kafkaProducer.connect(),
        redisClient.connect(),
    ])

    logger.info('Connected to Kafka and Redis')

    startRateLimitRefresher(
        redisClient,
        REDIS_RATE_LIMIT_KEY,
        GEMINI_TEXT_EMBEDDING_004_RATE_LIMIT_PER_MINUTE,
        60_000
    )

    kafkaConsumer.subscribe(async (payload) => {
        const rateLimit = await redisClient.get(REDIS_RATE_LIMIT_KEY)

        if (!rateLimit) {
            logger.error('Rate limit not set in Redis')
            return
        }

        const rateLimitValue = parseInt(rateLimit, 10)

        if (rateLimitValue <= 0) {
            logger.error('Rate limit exceeded')
            return
        }

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

        const { requestId, appId, chatId, question } = JSON.parse(
            value.toString()
        )

        if (!requestId || !appId || !chatId || !question) {
            return
        }

        const { embeddings } = await googleGenAI.models.embedContent({
            contents: question,
            model: 'text-embedding-004',
        })

        if (!embeddings || embeddings.length === 0) {
            logger.error('No embeddings found')
            return
        }
        const embedding = embeddings[0]

        kafkaProducer.sendMessage(
            JSON.stringify({
                requestId,
                appId,
                chatId,
                question,
                embedding,
            })
        )

        await redisClient.decr(REDIS_RATE_LIMIT_KEY)
    })
}

main()
