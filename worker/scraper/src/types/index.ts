import { ContentEmbedding } from '@google/genai'

export type RabbitMQMessage = {
    url: string
    mode: 'full' | 'single'
}

export type KafkaMessage = {
    metadata: {
        url: string
        websiteId: string
        sectionNo: number
    }
    embedding: ContentEmbedding[] | undefined
}
