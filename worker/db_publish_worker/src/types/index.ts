export type KAFKA_MESSAGE_METADATA = {
    url: string
    websiteId: string
    sectionNo: number
    title: string
    body: string
}

export type KAFKA_MESSAGE = {
    metadata: KAFKA_MESSAGE_METADATA
    embedding: {
        values: string[]
    }[]
}

export type CHAT_ANSWER_RESPONSE = {
    data: {
        answer: string
        question: string
        appId: string
        messageId: string
        chatId: string
        requestId: string
    }
    usageMetadata: {
        candidatesTokenCount: number
        promptTokenCount: number
        totalTokenCount: number
    }
}

export type WEBHOOK_MESSAGE = {
    appId: string
    webhookId: string
    data: {
        answer: string
        question: string
        appId: string
        messageId: string
        chatId: string
        requestId: string
    }
    webhook: {
        id: string
        url: string
        name: string
    }
    response: {
        status: number
        success: boolean
        message: string
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        data: any
    }
}

export type GroupedUrls = {
    group: string
    urls: string[]
}

export type INGESTION_MESSAGE = {
    appId: string
    urls: GroupedUrls[]
    metadata: {
        ingest: boolean
        ingestedBy: string
        ingestionMedium: string
    }
}

export type FIRST_TIME_MESSAGE = {
    appId: string
    urls: GroupedUrls[]
}
