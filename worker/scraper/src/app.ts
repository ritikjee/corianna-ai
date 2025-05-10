import 'dotenv/config'

import { Kafka } from './services/kafka'
import { ProcessData } from './services/process'
import { RabbitMQ } from './services/rabbitmq'
import { Scapper } from './services/scapper'
import { logger } from './utils/logger'

async function main() {
    const rabbitMQUrl = process.env.RABBITMQ_URL as string
    const kafkaBrokerUrl = process.env.KAFKA_BROKER_URL as string

    if (!rabbitMQUrl) {
        logger.error('RABBITMQ_URL environment variable is not set.')
        process.exit(1)
    }

    const rabbitmq = new RabbitMQ(rabbitMQUrl)
    const kafka = new Kafka([kafkaBrokerUrl])

    try {
        await rabbitmq.connect()
        await kafka.init()
        logger.info('Connected to RabbitMQ and Kafka')
    } catch (error) {
        logger.error('Error connecting to RabbitMQ or Kafka:', error)
        process.exit(1)
    }

    rabbitmq.consumeMessages('scrape_website', async (msg) => {
        if (msg) {
            const url = msg.content.toString()

            const urls = await Scapper.crawlAllInternalLinks(url)

            urls.forEach(async (url) => {
                await ProcessData.scrapeAndEmbed(
                    url,
                    1000,
                    Math.floor(Math.random() * 1000).toString()
                )
            })
        }
    })
}

main()
