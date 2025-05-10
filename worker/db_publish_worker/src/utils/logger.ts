import winston from 'winston'
import { isMainThread, threadId } from 'worker_threads'
import { format } from 'date-fns'

const springBootFormat = winston.format.printf(
    ({ level, message, service }) => {
        const now = new Date()
        const timestamp = now.toISOString().replace('Z', `${format(now, 'xx')}`)
        const capitalizedLevel = level.toUpperCase()
        const processId = process.pid || '75451'
        const threadName = isMainThread ? 'main' : 'worker'
        const threadIdValue = threadId || '0'

        return `${timestamp}  ${capitalizedLevel} ${processId} --- [${service}] [${threadName}] ${threadIdValue}    : ${message}`
    }
)

export const logger = winston.createLogger({
    level: 'info',
    format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.metadata({
            fillExcept: ['message', 'level', 'timestamp', 'service'],
        }),
        springBootFormat
    ),
    defaultMeta: { service: 'bot-service' },
    transports: [new winston.transports.Console()],
})
