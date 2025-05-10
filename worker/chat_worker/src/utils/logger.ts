import winston from 'winston'
import { isMainThread } from 'worker_threads'
import { format } from 'date-fns'
import path from 'path'

const loggerConfig = winston.format.printf(({ level, message, service }) => {
    const now = new Date()
    const timestamp = now.toISOString().replace('Z', `${format(now, 'xx')}`)
    const capitalizedLevel = level.toUpperCase()
    const processId = process.pid
    const threadName = isMainThread ? 'main' : 'worker'

    // Get the file path from the stack trace
    const stack = new Error().stack?.split('\n')
    const callerLine = stack?.[10] || stack?.[3] || '' // adjust based on depth
    const fileMatch = callerLine.match(/\((.*):(\d+):(\d+)\)/)
    const filePath = fileMatch
        ? `${path.basename(fileMatch[1])}:${fileMatch[2]}`
        : 'unknown'

    return `${timestamp}  ${capitalizedLevel} ${processId} --- [${service}] [${threadName}] [${filePath}] : ${message}`
})

export const logger = winston.createLogger({
    level: 'info',
    format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.metadata({
            fillExcept: ['message', 'level', 'timestamp', 'service'],
        }),
        loggerConfig
    ),
    defaultMeta: { service: 'bot-worker' },
    transports: [new winston.transports.Console()],
})
