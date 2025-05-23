import { Schema, model } from 'mongoose'
import { GroupedUrls } from '../types'

type APP_URLS = {
    appId: string
    urls: GroupedUrls[]
}

const appUrlsSchema = new Schema<APP_URLS>(
    {
        appId: { type: String, required: true },
        urls: [
            {
                group: { type: String, required: true },
                urls: [{ type: String, required: true }],
            },
        ],
    },
    {
        timestamps: true,
    }
)

const AppUrls = model<APP_URLS>('AppUrls', appUrlsSchema)

export default AppUrls
