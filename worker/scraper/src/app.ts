import 'dotenv/config'

import pLimit from 'p-limit'

import {
    GEMINI_TEXT_EMBEDDING_004_RATE_LIMIT_PER_MINUTE,
    REDIS_RATE_LIMIT_KEY,
} from './constants'
import { Kafka } from './services/kafka'
import { ProcessData } from './services/process'
import { RabbitMQ } from './services/rabbitmq'
import { RedisClient } from './services/redis'
import { Scapper } from './services/scapper'
import { groupUrlsByPath } from './utils/common'
import { startRateLimitRefresher } from './utils/helper'
import { logger } from './utils/logger'

async function main() {
    const rabbitMQUrl = process.env.RABBITMQ_URL as string
    const kafkaBrokerUrl = process.env.KAFKA_BROKER_URL as string

    if (!rabbitMQUrl) {
        logger.error('RABBITMQ_URL environment variable is not set.')
        process.exit(1)
    }

    if (!kafkaBrokerUrl) {
        logger.error('KAFKA_BROKER_URL environment variable is not set.')
        process.exit(1)
    }

    logger.info('Connecting to RabbitMQ and Kafka...')

    const rabbitmq = new RabbitMQ(rabbitMQUrl)
    const kafka = new Kafka([kafkaBrokerUrl])
    const redis = new RedisClient()

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

            const data = msg.content.toString()

            const { url, mode, appId, metadata } = JSON.parse(data)

            let urls: string[] = []

            if (mode == 'full') {
                urls = await Scapper.crawlAllInternalLinks(url)
            }

            if (mode == 'single') {
                urls = [url]
            }

            if (mode == 'pattern') {
                const patternUrls = await Scapper.crawlAllInternalLinks(url)
                urls = patternUrls.filter((patternUrl) => {
                    return patternUrl.includes(url.split('*')[1])
                })
            }

            const limit = pLimit(5)

            const tasks = urls.map((url) =>
                limit(() => ProcessData.scrapeAndEmbed(url, 2000, appId))
            )

            await Promise.all(tasks)

            if (metadata?.ingest) {
                const groupedUrls = groupUrlsByPath(urls)
                kafka.sendMessage(
                    'ingested-url',
                    JSON.stringify({ appId, urls: groupedUrls, metadata })
                )
            }

            if (metadata?.firstTime) {
                const groupedUrls = groupUrlsByPath(urls)
                kafka.sendMessage(
                    'first-time-url',
                    JSON.stringify({ appId, urls: groupedUrls })
                )
            }

            await rabbitmq.ack(msg)
        }
    })
}

main()
