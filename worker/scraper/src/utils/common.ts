type GroupedUrls = {
    group: string
    urls: string[]
}

export function groupUrlsByPath(urls: string[]): GroupedUrls[] {
    const groups: Record<string, string[]> = {}

    urls.forEach((url) => {
        // Extract the first path segment after the domain
        const match =
            url.match(/^https?:\/\/[^/]+\/([^/]+)/) ||
            url.match(/^[^/]+\/([^/]+)/)
        const basePath = match ? match[1] : 'unknown'
        const groupName = basePath.charAt(0).toUpperCase() + basePath.slice(1)

        if (!groups[groupName]) {
            groups[groupName] = []
        }

        groups[groupName].push(url)
    })

    // Convert to desired array format

    return Object.entries(groups).map(([group, urls]) => ({
        group,
        urls,
    }))
}
