import { v4 as uuid } from 'uuid'
import { ChromaDB } from './services/chromadb'
import { googleGenAI } from './services/gemini'
import { KafkaComnsumer } from './services/kafka/consumer'
import { KafkaProducer } from './services/kafka/producer'
import { RedisClient } from './services/redis'
import { CHROMA_DB_METADATA } from './types'
import { formatWebsiteContent } from './utils/helper'
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
        'chat-questions'
    )

    const chatAnswerProducer = new KafkaProducer(
        [KAFKA_PRODUCER_BROKER],
        'chat-answers'
    )

    const webhookWorkerProducer = new KafkaProducer(
        [KAFKA_PRODUCER_BROKER],
        'webhook-worker'
    )

    const redisClient = new RedisClient(REDIS_CONNECTION_URI)

    const chromaClient = new ChromaDB()

    await Promise.all([
        kafkaConsumer.connect(),
        chatAnswerProducer.init(),
        webhookWorkerProducer.init(),
        chatAnswerProducer.connect(),
        webhookWorkerProducer.connect(),
        redisClient.connect(),
        chromaClient.init(),
    ])

    logger.info('Connected to Kafka, Redis and ChromaDB')

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

        const data = JSON.parse(value.toString())

        const { embeddings } = await googleGenAI.models.embedContent({
            model: 'text-embedding-004',
            contents: [data.question],
        })

        if (!embeddings || !embeddings[0]) {
            logger.error('No embeddings found')
            return
        }

        const query = embeddings[0].values

        if (!query) {
            logger.error('No query found')
            return
        }

        const results = await chromaClient.query(query, 5)

        const { candidates, usageMetadata } =
            await googleGenAI.models.generateContent({
                model: 'gemini-1.5-flash-8b',
                contents: [
                    `You are a helpful assistant. Answer the question based on the following content:\n\n${formatWebsiteContent(results.metadatas as CHROMA_DB_METADATA[][])}\n\nQuestion: ${data.question}\nAnswer:`,
                ],
                config: {
                    systemInstruction: {
                        role: 'system',
                        text: 'You are a helpful assistant. Answer the question based on the following content and give detailed answer explaining each point. Note try to be polite and friendly.Use emojis where possible.',
                    },
                },
            })

        const answer =
            candidates?.[0]?.content ||
            'Sorry, I could not find an answer to your question at current point of time. Please try again later.'

        const messageId = uuid()

        chatAnswerProducer.sendMessage(
            JSON.stringify({
                data: { answer, messageId, ...data },
                usageMetadata,
            })
        )

        webhookWorkerProducer.sendMessage(
            JSON.stringify({ ...data, answer, messageId })
        )
    })
}

main()
