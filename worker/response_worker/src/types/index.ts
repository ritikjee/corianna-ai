type KAFKA_DATA_PAYLOAD_RESPONSE = {
    answer: string
    status: string
    question: string
    requestId: string
    appId: string
    messageId: string
}
type KAFKA_DATA_PAYLOAD_METADATA = {
    type: string
    accessToken?: string
    webhookUrl?: string
    userToken?: string
}

export type KAFKA_DATA_PAYLOAD = {
    response: KAFKA_DATA_PAYLOAD_RESPONSE
    metadata: KAFKA_DATA_PAYLOAD_METADATA
    usageMetadata: unknown
}
