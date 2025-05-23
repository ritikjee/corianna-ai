import { Schema, model } from 'mongoose'
import { GroupedUrls } from '../types'

type INGESTION_RESPONSE = {
    appId: string
    urls: GroupedUrls[]
    ingestedBy: string
    ingestionMedium: string
}

const ingestionResponseSchema = new Schema<INGESTION_RESPONSE>(
    {
        appId: { type: String, required: true },
        urls: [
            {
                group: { type: String, required: true },
                urls: [{ type: String, required: true }],
            },
        ],
        ingestedBy: { type: String, required: true },
        ingestionMedium: { type: String, required: true },
    },
    {
        timestamps: true,
    }
)

const IngestionResponse = model<INGESTION_RESPONSE>(
    'IngestionResponse',
    ingestionResponseSchema
)

export default IngestionResponse
