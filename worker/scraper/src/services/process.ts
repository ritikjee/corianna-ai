import { ContentEmbedding } from '@google/genai'
import * as cheerio from 'cheerio'
import { fetcher } from '../utils/fetcher'
import { googleGenAI } from '../utils/gemini'
import { Kafka } from './kafka'
import { logger } from '../utils/logger'
import { RedisClient } from './redis'
import { REDIS_RATE_LIMIT_KEY } from '../constants'

export class ProcessData {
    private static kafka = new Kafka([process.env.KAFKA_BROKER_URL as string])

    private static estimateTokenCount = (text: string) =>
        Math.ceil(text.length / 4)

    private static chunkText(text: string, tokenLimit: number): string[] {
        const words = text.split(/\s+/)
        const chunks: string[] = []
        let currentChunk: string[] = []
        let tokenCount = 0

        for (const word of words) {
            const wordTokenCount = ProcessData.estimateTokenCount(word + ' ')
            if (tokenCount + wordTokenCount > tokenLimit) {
                chunks.push(currentChunk.join(' '))
                currentChunk = []
                tokenCount = 0
            }
            currentChunk.push(word)
            tokenCount += wordTokenCount
        }

        if (currentChunk.length) {
            chunks.push(currentChunk.join(' '))
        }

        return chunks
    }
    private static async scrapeWebsite(url: string, token_limit: number) {
        const { data, error } = await fetcher<string>({
            url,
            method: 'GET',
        })

        if (error) {
            logger.error('Error fetching the URL:', error)
            return
        }

        const $ = cheerio.load(data)
        const title = $('title').text()

        $('script, style, nav, header, footer').remove()
        const $body = $('body')
        const textBlocks: string[] = []

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        function extractText(element: any) {
            const tag = element.tagName?.toLowerCase()
            if (
                !tag ||
                ['script', 'style', 'nav', 'header', 'footer'].includes(tag)
            )
                return

            const $el = $(element)

            if (tag === 'a') {
                const href = $el.attr('href') || ''
                const text = $el.text().trim()
                if (text) {
                    textBlocks.push(`${text} (${href})`)
                }
                return
            }

            if ($el.children().length === 0) {
                const text = $el.text().trim()
                if (text) textBlocks.push(text)
            } else {
                $el.contents().each((_, child) => {
                    if (child.type === 'text') {
                        const text = $(child).text().trim()
                        if (text) textBlocks.push(text)
                    } else {
                        extractText(child)
                    }
                })
            }
        }

        $body.children().each((_, el) => extractText(el))

        const fullText = textBlocks.join(' ').replace(/\s+/g, ' ')
        const chunks = ProcessData.chunkText(fullText, token_limit)

        return {
            title,
            chunks,
        }
    }

    private static async generateEmbeddings(
        chunks: string[],
        url: string,
        websiteId: string,
        baseSectionNo: number = 0
    ) {
        const embeddingsResults: {
            metadata: { url: string; websiteId: string; sectionNo: number }
            embedding: ContentEmbedding[] | undefined
        }[] = []

        const redis = new RedisClient()

        try {
            for (let i = 0; i < chunks.length; i++) {
                let rate = Number(await redis.get(REDIS_RATE_LIMIT_KEY))

                // if (!rate) {
                //     while (!rate) {
                //         logger.info(
                //             'Rate limit reached. Retrying in 2 seconds...'
                //         )

                //         await new Promise((resolve) =>
                //             setTimeout(resolve, 5000)
                //         )

                //         rate = Number(await redis.get(REDIS_RATE_LIMIT_KEY))
                //     }
                // }

                const chunk = chunks[i]
                const { embeddings } = await googleGenAI.models.embedContent({
                    model: 'text-embedding-004',
                    contents: chunk,
                })

                await redis.decr(REDIS_RATE_LIMIT_KEY)

                embeddingsResults.push({
                    metadata: {
                        url,
                        websiteId,
                        sectionNo: baseSectionNo + i,
                    },
                    embedding: embeddings,
                })
            }

            return embeddingsResults
        } catch (error) {
            logger.error('Error generating embeddings:', error)
            throw error
        }
    }

    static async scrapeAndEmbed(
        url: string,
        tokenLimit: number,
        websiteId: string
    ) {
        const scrapedData = await this.scrapeWebsite(url, tokenLimit)

        if (!scrapedData) {
            return null
        }

        const embeddedChunks = await this.generateEmbeddings(
            scrapedData.chunks,
            url,
            websiteId
        )

        if (!embeddedChunks) {
            return null
        }

        await this.kafka.connect()

        await this.kafka.sendMessagesInBatches(embeddedChunks)

        await this.kafka.disconnect()

        return {
            websiteId,
            url,
        }
    }
}
