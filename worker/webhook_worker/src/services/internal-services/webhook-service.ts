import { Webhook } from '../../types'
import { fetcher } from '../../utils/fetcher'
import { logger } from '../../utils/logger'
import { RedisClient } from '../redis'

export class WebhookService {
    static redis = new RedisClient()

    static async getWebhooks(appId: string) {
        if (!process.env.APP_SERVICE_URL) {
            logger.error('APP_SERVICE_URL is not defined')
            process.exit(1)
        }

        let webhooks: Webhook[] = []

        const redisValue = await this.redis.get(`webhooks::${appId}`)
        if (redisValue) {
            webhooks = JSON.parse(redisValue)
            logger.info('Webhooks fetched from Redis')
        } else {
            const { data, error } = await fetcher<Webhook[]>({
                url: `{process.env.APP_SERVICE_URL}/api/internal-services/webhooks`,
                method: 'GET',
                params: {
                    appId,
                },
            })

            if (error) {
                logger.error('Error fetching webhooks:', error)
                return []
            }
            if (!data) {
                logger.error('No data received from webhooks endpoint')
                return []
            }
            webhooks = data
        }

        await this.redis.set(
            `webhooks::${appId}`,
            JSON.stringify(webhooks),
            60 * 15
        )

        return webhooks
    }
}
