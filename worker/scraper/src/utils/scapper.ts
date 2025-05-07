import { fetcher } from './fetcher'
import * as cheerio from 'cheerio'

export class Scapper {
    private static estimateTokenCount = (text: string) =>
        Math.ceil(text.length / 4)

    private static chunkText(text: string, tokenLimit: number): string[] {
        const words = text.split(/\s+/)
        const chunks: string[] = []
        let currentChunk: string[] = []
        let tokenCount = 0

        for (const word of words) {
            const wordTokenCount = Scapper.estimateTokenCount(word + ' ')
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

    private static normalizeUrl = (
        href: string,
        base: string
    ): string | null => {
        try {
            const baseUrl = new URL(base)
            if (!href || href.startsWith('#') || href.startsWith('javascript:'))
                return null
            const cleanHref = href.split('?')[0].split('#')[0]
            const fullUrl = new URL(cleanHref, baseUrl)
            return fullUrl.origin === baseUrl.origin ? fullUrl.href : null
        } catch {
            return null
        }
    }

    private static async extractLinks(url: string): Promise<string[]> {
        const { data, error } = await fetcher<string>({ url, method: 'GET' })
        if (error || !data) return []

        const $ = cheerio.load(data)
        const links = new Set<string>()

        $('a[href]').each((_, el) => {
            const rawHref = $(el).attr('href')?.trim()
            const normalized = Scapper.normalizeUrl(rawHref || '', url)
            if (normalized) links.add(normalized)
        })

        return Array.from(links)
    }

    static async scrapeWebsite(url: string, token_limit: number) {
        const { data, error } = await fetcher<string>({
            url,
            method: 'GET',
        })

        if (error) {
            console.error('Error fetching the URL:', error)
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
        const chunks = Scapper.chunkText(fullText, token_limit)

        return {
            title,
            chunks,
        }
    }
    static async crawlAllInternalLinks(
        startUrl: string
    ): Promise<Array<string>> {
        const visited = new Set<string>()
        const queue: string[] = [startUrl]
        // Create a set to track URLs that are already in the queue
        const inQueue = new Set<string>([startUrl])

        while (queue.length > 0) {
            const url = queue.shift()!
            // Remove from inQueue set as we're processing it now
            inQueue.delete(url)

            if (visited.has(url)) continue
            visited.add(url)

            try {
                const internalLinks = await Scapper.extractLinks(url)
                for (const link of internalLinks) {
                    if (!visited.has(link) && !inQueue.has(link)) {
                        queue.push(link)
                        inQueue.add(link)
                    }
                }
            } catch (e) {
                console.warn(`Failed to crawl ${url}:`, e)
            }
        }

        return Array.from(visited)
    }
}
