import { GoogleGenAI } from '@google/genai'

import 'dotenv/config'

if (!process.env.GOOGLE_GENAI_API_KEY) {
    throw new Error('GOOGLE_GENAI_API_KEY environment variable is not set.')
}

export const googleGenAI = new GoogleGenAI({
    apiKey: process.env.GOOGLE_GENAI_API_KEY,
})
