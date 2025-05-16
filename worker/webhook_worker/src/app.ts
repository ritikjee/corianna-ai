import pLimit from 'p-limit'

import { WebhookService } from './services/internal-services/webhook-service'
import { KafkaComnsumer } from './services/kafka/consumer'
import { KafkaProducer } from './services/kafka/producer'
import { logger } from './utils/logger'
import { sendWebhook } from './utils/webhook'
import { encodeValueWithHMAC } from './utils/encode'

async function main() {
    const KAFKA_CLIENT_BROKER = process.env.KAFKA_CLIENT_BROKER
    const KAFKA_PRODUCER_BROKER = process.env.KAFKA_PRODUCER_BROKER

    if (!KAFKA_CLIENT_BROKER || !KAFKA_PRODUCER_BROKER) {
        logger.error('Missing environment variables')
        process.exit(1)
    }

    const kafkaConsumer = new KafkaComnsumer(
        [KAFKA_CLIENT_BROKER],
        'webhook-worker'
    )

    const webhookResponseProducer = new KafkaProducer(
        [KAFKA_PRODUCER_BROKER],
        'webhook-responses'
    )

    await Promise.all([
        kafkaConsumer.connect(),
        webhookResponseProducer.connect(),
    ])

    logger.info('Connected to Kafka')

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

        if (!data.appId) {
            logger.error('No appId found in message')
            return
        }

        const webhooks = await WebhookService.getWebhooks(data.appId)

        const webhookResponses: unknown[] = []

        const limit = pLimit(5)

        const webhookPromises = webhooks.map((webhook) => {
            return limit(async () => {
                const response = await sendWebhook(
                    webhook.url,
                    encodeValueWithHMAC(value, webhook.token),
                    webhook.headers
                )
                if (response) {
                    webhookResponses.push({
                        data,
                        webhook: {
                            id: webhook.id,
                            url: webhook.url,
                            name: webhook.name,
                        },
                        appId: data.appId,
                        webhookId: webhook.id,
                        response,
                    })
                }
            })
        })

        await Promise.all(webhookPromises)

        webhookResponseProducer.sendMessage(
            JSON.stringify({
                webhookResponses,
            })
        )
    })
}

main()
