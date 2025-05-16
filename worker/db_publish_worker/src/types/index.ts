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
