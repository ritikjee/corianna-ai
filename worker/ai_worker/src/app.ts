import { ChromaDB } from './services/chromadb'
import { googleGenAI } from './services/gemini'
import { KafkaComnsumer } from './services/kafka/consumer'
import { KafkaProducer } from './services/kafka/producer'
import { RedisClient } from './services/redis'
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

    const kafkaProducer = new KafkaProducer(
        [KAFKA_PRODUCER_BROKER],
        'chat-answers'
    )

    const redisClient = new RedisClient(REDIS_CONNECTION_URI)

    const chromaClient = new ChromaDB()

    await Promise.all([
        kafkaConsumer.connect(),
        kafkaProducer.connect(),
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

        const { question } = JSON.parse(value.toString())

        const { embeddings } = await googleGenAI.models.embedContent({
            model: 'text-embedding-004',
            contents: [question],
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

        console.log('Results:', results)
    })
}

main()
