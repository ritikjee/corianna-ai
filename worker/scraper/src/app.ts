import 'dotenv/config'

import pLimit from 'p-limit'

import { Kafka } from './services/kafka'
import { ProcessData } from './services/process'
import { RabbitMQ } from './services/rabbitmq'
import { Scapper } from './services/scapper'
import { logger } from './utils/logger'
import { RedisClient } from './services/redis'
import {
    GEMINI_TEXT_EMBEDDING_004_RATE_LIMIT_PER_MINUTE,
    REDIS_RATE_LIMIT_KEY,
} from './constants'
import { startRateLimitRefresher } from './utils/helper'

async function main() {
    const rabbitMQUrl = process.env.RABBITMQ_URL as string
    const kafkaBrokerUrl = process.env.KAFKA_BROKER_URL as string
    const redisURL = process.env.REDIS_URL as string

    if (!rabbitMQUrl) {
        logger.error('RABBITMQ_URL environment variable is not set.')
        process.exit(1)
    }

    if (!kafkaBrokerUrl) {
        logger.error('KAFKA_BROKER_URL environment variable is not set.')
        process.exit(1)
    }
    if (!redisURL) {
        logger.error('REDIS_URL environment variable is not set.')
        process.exit(1)
    }

    logger.info('Connecting to RabbitMQ and Kafka...')

    const rabbitmq = new RabbitMQ(rabbitMQUrl)
    const kafka = new Kafka([kafkaBrokerUrl])
    const redis = new RedisClient(redisURL)

    try {
        await rabbitmq.connect()
        await kafka.init()
        await redis.connect()
        logger.info('Connected to RabbitMQ, Kafka and Redis successfully.')
    } catch (error) {
        logger.error('Error connecting to RabbitMQ or Kafka or Redis:', error)
        process.exit(1)
    }

    startRateLimitRefresher(
        redis,
        REDIS_RATE_LIMIT_KEY,
        GEMINI_TEXT_EMBEDDING_004_RATE_LIMIT_PER_MINUTE,
        60_000
    )

    rabbitmq.consumeMessages('scrape_website', async (msg) => {
        if (msg) {
            const rateLimit = await redis.get(REDIS_RATE_LIMIT_KEY)

            if (!rateLimit) {
                logger.info('Rate limit reached, skipping message.')
                return
            }

            const rate = Number.parseInt(rateLimit, 10)

            if (rate < 0) {
                logger.info('Rate limit reached, skipping message.')
                return
            }

            const url = msg.content.toString()

            const urls = await Scapper.crawlAllInternalLinks(url)

            const limit = pLimit(5)

            const tasks = urls.map((url) =>
                limit(() =>
                    ProcessData.scrapeAndEmbed(
                        url,
                        1000,
                        Math.floor(Math.random() * 1000).toString()
                    )
                )
            )

            await Promise.all(tasks)

            await rabbitmq.ack(msg)
        }
    })
}

main()
