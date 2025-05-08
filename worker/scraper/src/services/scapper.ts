import * as cheerio from 'cheerio'
import { fetcher } from '../utils/fetcher'

export class Scapper {
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
