import { logLevel as KafkaLogLevel, LogEntry } from 'kafkajs'
import { logger } from '../../utils/logger'

export const kafkaLogCreator = () => {
    return ({ namespace, level, label, log }: LogEntry) => {
        const { message, ...extra } = log

        switch (level) {
            case KafkaLogLevel.ERROR:
                logger.error(message, { namespace, label, ...extra })
                break
            case KafkaLogLevel.WARN:
                logger.warn(message, { namespace, label, ...extra })
                break
            case KafkaLogLevel.INFO:
                logger.info(message, { namespace, label, ...extra })
                break
            case KafkaLogLevel.DEBUG:
                logger.debug(message, { namespace, label, ...extra })
                break
            default:
                logger.info(message, { namespace, label, ...extra })
                break
        }
    }
}
