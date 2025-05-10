// utils/rateLimiter.ts

import { RedisClient } from '../services/redis'
import { logger } from './logger'

export async function startRateLimitRefresher(
    redisClient: RedisClient,
    key: string,
    limit: number,
    intervalMs: number
) {
    setInterval(async () => {
        try {
            await redisClient.set(key, limit)
        } catch (error) {
            logger.error(
                `[RateLimiter] Failed to reset rate limit key "${key}"`,
                error
            )
        }
    }, intervalMs)
}
