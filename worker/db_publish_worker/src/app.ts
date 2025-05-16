import 'dotenv/config'

import { KafkaClient } from './services/kafka'
import { KAFKA_MESSAGE } from './types'
import { ChromaDB } from './services/chromadb'
import { logger } from './utils/logger'

async function main() {
    const KAFKA_BROKER_URL = process.env.KAFKA_BROKER_URL

    if (!KAFKA_BROKER_URL) {
        throw new Error('KAFKA_BROKER_URL is not defined')
    }

    const client = new KafkaClient([KAFKA_BROKER_URL])
    const chromaClient = new ChromaDB()

    await client.connect()
    await chromaClient.init()

    logger.info('Connected to Kafka and ChromaDB')

    const messageBuffer: KAFKA_MESSAGE[] = []
    const MAX_BATCH_SIZE = 100
    const FLUSH_INTERVAL_MS = 30_000

    let flushTimeout: NodeJS.Timeout

    const flushMessages = async () => {
        if (messageBuffer.length === 0) return
        const batch = messageBuffer.splice(0, messageBuffer.length) // clear buffer
        await chromaClient.addDocuments(batch)
        logger.info(`Flushed ${batch.length} messages to ChromaDB`)
    }

    const scheduleFlush = () => {
        logger.info(`Scheduling flush in ${FLUSH_INTERVAL_MS}ms`)
        clearTimeout(flushTimeout) // reset timer
        flushTimeout = setTimeout(async () => {
            await flushMessages()
            scheduleFlush() // reschedule
        }, FLUSH_INTERVAL_MS)
    }

    scheduleFlush()

    await client.init(async (payload) => {
        const {
            message: { value },
        } = payload

        if (!value || !value.toString()) {
            return
        }

        try {
            const parsed: KAFKA_MESSAGE = JSON.parse(value.toString())

            messageBuffer.push(parsed)

            if (messageBuffer.length >= MAX_BATCH_SIZE) {
                await flushMessages()
                scheduleFlush()
            }
        } catch (err) {
            logger.error('Error parsing message:', err)
        }
    })
}

main()
