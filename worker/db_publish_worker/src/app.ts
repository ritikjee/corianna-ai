import 'dotenv/config'

import Chat from './models/chat'
import Webhook from './models/webhook'
import { ChromaDB } from './services/chromadb'
import { KafkaClient } from './services/kafka'
import { MongoDB } from './services/mongodb'
import { CHAT_ANSWER_RESPONSE, KAFKA_MESSAGE, WEBHOOK_MESSAGE } from './types'
import { createFlushScheduler } from './utils/flush-scheduler'
import { logger } from './utils/logger'

async function main() {
    const KAFKA_BROKER_URL = process.env.KAFKA_BROKER_URL

    if (!KAFKA_BROKER_URL) {
        throw new Error('KAFKA_BROKER_URL is not defined')
    }

    const client = new KafkaClient([KAFKA_BROKER_URL])
    const chromaClient = new ChromaDB()
    const mongoClient = new MongoDB()

    await Promise.all([
        client.connect(),
        chromaClient.init(),
        mongoClient.connect(),
    ])

    logger.info('Connected to Kafka, MongoDB and ChromaDB')

    await client.subscribe(
        ['web-scrapper-embeddings', 'chat-answers', 'webhook-responses'],
        async (payload) => {
            const {
                message: { value },
                topic,
            } = payload

            if (!value || !value.toString()) {
                return
            }

            switch (topic) {
                case 'web-scrapper-embeddings': {
                    try {
                        const parsed: KAFKA_MESSAGE = JSON.parse(
                            value.toString()
                        )

                        const scheduler = createFlushScheduler<KAFKA_MESSAGE>(
                            async (batch) => {
                                await chromaClient.addDocuments(batch)
                                logger.info(
                                    `Flushed ${batch.length} messages to ChromaDB`
                                )
                            },
                            30_000,
                            100
                        )

                        scheduler.add(parsed)
                    } catch (err) {
                        logger.error('Error message:', err)
                    }
                    break
                }
                case 'chat-answers': {
                    try {
                        const parsed = JSON.parse(value.toString())

                        const scheduler =
                            createFlushScheduler<CHAT_ANSWER_RESPONSE>(
                                async (batch) => {
                                    await Chat.insertMany(
                                        batch.map((item) => ({
                                            answer: item.data.answer,
                                            question: item.data.question,
                                            appId: item.data.appId,
                                            messageId: item.data.messageId,
                                            chatId: item.data.chatId,
                                            candidatesTokenCount:
                                                item.usageMetadata
                                                    .candidatesTokenCount,
                                            promptTokenCount:
                                                item.usageMetadata
                                                    .promptTokenCount,
                                            totalTokenCount:
                                                item.usageMetadata
                                                    .totalTokenCount,
                                        }))
                                    )

                                    logger.info(
                                        `Flushed ${batch.length} messages to MongoDB`
                                    )
                                },
                                30_000,
                                100
                            )

                        scheduler.add(parsed)
                    } catch (err) {
                        logger.error('Error message:', err)
                    }
                    break
                }
                case 'webhook-responses': {
                    try {
                        const parsed = JSON.parse(value.toString())

                        const scheduler = createFlushScheduler<WEBHOOK_MESSAGE>(
                            async (batch) => {
                                await Webhook.insertMany(
                                    batch.map((item) => ({
                                        appId: item.appId,
                                        webhookId: item.webhook.id,
                                        messageId: item.data.messageId,
                                        chatId: item.data.chatId,
                                        url: item.webhook.url,
                                        name: item.webhook.name,
                                        response: {
                                            status: item.response.status,
                                            success: item.response.success,
                                            message: item.response.message,
                                            data: item.response.data,
                                        },
                                    }))
                                )
                                logger.info(
                                    `Flushed ${batch.length} messages to MongoDB`
                                )
                            },
                            30_000,
                            100
                        )

                        scheduler.add(parsed)
                    } catch (err) {
                        logger.error('Error message:', err)
                    }
                    break
                }
                default: {
                    logger.error(
                        `Unknown topic ${topic} with message ${value.toString()}`
                    )
                    break
                }
            }
        }
    )
}

main()
