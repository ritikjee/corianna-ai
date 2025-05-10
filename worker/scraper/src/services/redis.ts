import { createClient } from 'redis'
import {
    GEMINI_TEXT_EMBEDDING_004_RATE_LIMIT_PER_MINUTE,
    REDIS_RATE_LIMIT_KEY,
} from '../constants'
import { logger } from '../utils/logger'

export class RedisClient {
    private client: ReturnType<typeof createClient>

    constructor(url: string) {
        this.client = createClient({ url })
    }

    async connect() {
        try {
            await this.client.connect()

            await this.client.set(
                REDIS_RATE_LIMIT_KEY,
                GEMINI_TEXT_EMBEDDING_004_RATE_LIMIT_PER_MINUTE,
                {
                    EX: 60,
                }
            )
        } catch (error) {
            logger.error('Error connecting to Redis:', error)
            process.exit(1)
        }
    }

    async set(key: string, value: string | number, expiry: number = 60) {
        try {
            await this.client.set(key, value, {
                EX: expiry,
            })
        } catch (error) {
            logger.error('Error setting key in Redis:', error)
        }
    }

    async get(key: string) {
        try {
            const value = await this.client.get(key)
            return value
        } catch (error) {
            logger.error('Error getting key from Redis:', error)
            return null
        }
    }

    async decr(key: string) {
        try {
            const value = await this.client.decr(key)
            return value
        } catch (error) {
            logger.error('Error decrementing key in Redis:', error)
            return null
        }
    }

    async disconnect() {
        await this.client.quit()
        logger.info('Redis client disconnected')
    }
}
