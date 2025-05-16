import mongoose from 'mongoose'
import { logger } from '../utils/logger'

export class MongoDB {
    private MONGO_URI: string = process.env.MONGO_URI as string

    constructor() {
        if (!this.MONGO_URI) {
            throw new Error('MONGO_URI is not defined')
        }
    }

    async connect() {
        try {
            await mongoose.connect(this.MONGO_URI)
        } catch (err) {
            logger.error('Error connecting to MongoDB:', err)
        }
    }
}
