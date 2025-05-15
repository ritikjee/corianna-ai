import { ChromaClient, Collection } from 'chromadb'
import { CHROMADB_COLLECTION_NAME } from '../constants'
import { logger } from '../utils/logger'

export class ChromaDB {
    private client: ChromaClient
    private collection!: Collection

    CHROMADB_URI = process.env.CHROMADB_URI as string

    constructor() {
        if (!this.CHROMADB_URI) {
            throw new Error('CHROMADB_URI is not defined')
        }

        this.client = new ChromaClient({
            path: this.CHROMADB_URI,
        })
    }

    async init() {
        try {
            this.client.api.heartbeat()

            this.collection = await this.client.getOrCreateCollection({
                name: CHROMADB_COLLECTION_NAME,
            })
        } catch (error) {
            logger.error('Error initializing ChromaDB:', error)
            process.exit(1)
        }
    }

    async query(query: number[], nResults: number = 5) {
        try {
            const results = await this.collection.query({
                queryEmbeddings: query,
                nResults: nResults,
            })

            return results
        } catch (error) {
            logger.error('Error querying ChromaDB:', error)
            throw error
        }
    }
}
