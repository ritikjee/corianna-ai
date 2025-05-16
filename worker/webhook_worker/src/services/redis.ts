import { createClient } from 'redis'
import { logger } from '../utils/logger'

export class RedisClient {
    private client: ReturnType<typeof createClient>
    private url = process.env.REDIS_URL

    constructor() {
        if (!this.url) {
            logger.error('REDIS_URL is not defined')
            process.exit(1)
        }

        this.client = createClient({ url: this.url })
    }

    async connect() {
        try {
            await this.client.connect()
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
