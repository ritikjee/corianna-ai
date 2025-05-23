import { RedisClient } from '../services/redis'
import { KAFKA_DATA_PAYLOAD } from '../types'
import { logger } from './logger'

export class MessageHandler {
    static async handleMessage(
        redisClient: RedisClient,
        data: KAFKA_DATA_PAYLOAD
    ) {
        const { metadata, response } = data

        const { type } = metadata

        switch (type) {
            case 'chat':
                redisClient.set(
                    `answer:${data.response.requestId}`,
                    JSON.stringify(response)
                )
                break
            default:
                logger.error('Unknown message type:', type)
        }
    }
}
