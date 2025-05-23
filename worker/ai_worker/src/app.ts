import { v4 as uuid } from 'uuid'
import { ChromaDB } from './services/chromadb'
import { googleGenAI } from './services/gemini'
import { KafkaComnsumer } from './services/kafka/consumer'
import { KafkaProducer } from './services/kafka/producer'
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
        'chat-questions-embeddings'
    )

    const chatAnswerProducer = new KafkaProducer(
        [KAFKA_PRODUCER_BROKER],
        'response-worker'
    )

    const chromaClient = new ChromaDB()

    await Promise.all([
        kafkaConsumer.connect(),
        chatAnswerProducer.init(),
        chatAnswerProducer.connect(),
        chromaClient.init(),
    ])

    logger.info('Connected to Kafka, Redis and ChromaDB')

    kafkaConsumer.subscribe(async (payload) => {
        const {
            message: { value, offset },
            partition,
            topic,
        } = payload

        logger.info('Received message from Kafka', {
            value,
            offset,
            partition,
            topic,
        })

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

        try {
            const { embedding } = data

            if (!embedding || !embedding.values) {
                logger.error('No embeddings found')
                return
            }

            const query = embedding?.values

            if (!query) {
                logger.error('No query found')
                return
            }

            const results = await chromaClient.query(query, 5, data.appId)

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

            const answer: {
                answer: string
                status: string
            } = {
                answer: '',
                status: 'error',
            }

            if (candidates?.[0]?.content?.parts?.[0]?.text) {
                answer.answer = candidates[0].content.parts[0].text
                answer.status = 'success'
            } else {
                answer.answer =
                    'Sorry, I could not find an answer to your question.'
                answer.status = 'error'
            }

            const messageId = uuid()

            const payloadToSend = {
                response: {
                    answer: answer.answer,
                    status: answer.status,
                    question: data.question,
                    requestId: data.requestId,
                    appId: data.appId,
                    messageId,
                },
                usageMetadata,
                metadata: {
                    ...data.metadata,
                },
            }

            logger.info('Sending answer to Kafka', {
                payload: payloadToSend,
            })

            await chatAnswerProducer.sendMessage(JSON.stringify(payloadToSend))

            logger.info('Answer sent to Kafka', {
                payload: payloadToSend,
            })
        } catch (error) {
            logger.error('Error processing message', {
                error,
                data,
            })
        }
    })
}

main()
