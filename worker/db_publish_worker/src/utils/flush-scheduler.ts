type FlushScheduler<T> = {
    add: (item: T) => void
    stop: () => void
}

export function createFlushScheduler<T>(
    flushFn: (batch: T[]) => Promise<void>,
    flushIntervalMs: number,
    maxBatchSize: number
): FlushScheduler<T> {
    const buffer: T[] = []
    let timeout: NodeJS.Timeout | null = null

    const flush = async () => {
        if (buffer.length === 0) return
        const batch = buffer.splice(0, buffer.length)
        try {
            await flushFn(batch)
        } catch (err) {
            console.error('Flush failed:', err)
            // Re-add messages if needed (optional)
            buffer.unshift(...batch)
        }
    }

    const schedule = () => {
        if (timeout) clearTimeout(timeout)
        timeout = setTimeout(async () => {
            await flush()
            schedule()
        }, flushIntervalMs)
    }

    const add = (item: T) => {
        buffer.push(item)
        if (buffer.length >= maxBatchSize) {
            if (timeout) clearTimeout(timeout)
            flush().then(schedule)
        }
    }

    const stop = () => {
        if (timeout) clearTimeout(timeout)
        timeout = null
    }

    schedule()

    return { add, stop }
}
