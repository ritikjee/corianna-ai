import { ChromaClient, Collection } from 'chromadb'
import { CHROMADB_COLLECTION_NAME } from '../constants'
import { KAFKA_MESSAGE, KAFKA_MESSAGE_METADATA } from '../types'
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

    async addDocuments(messages: KAFKA_MESSAGE[]) {
        try {
            if (!this.collection) {
                throw new Error('ChromaDB collection is not initialized')
            }

            const ids: string[] = []
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const documents: any = []
            const metadatas: KAFKA_MESSAGE_METADATA[] = []

            for (const message of messages) {
                const { embedding, metadata } = message

                if (!embedding?.[0]?.values?.length) {
                    logger.warn(
                        'Skipping message due to missing embedding:',
                        metadata?.url
                    )
                    continue
                }

                ids.push(metadata.url)
                documents.push(embedding[0].values)
                metadatas.push(metadata)
            }

            if (ids.length === 0) {
                console.warn('No valid documents to add to ChromaDB.')
                return
            }

            await this.collection.upsert({
                ids,
                embeddings: documents,
                metadatas,
            })

            logger.info(
                `Added ${ids.length} documents to ChromaDB collection ${CHROMADB_COLLECTION_NAME}`
            )
        } catch (error) {
            logger.error('Error adding documents to ChromaDB:', error)
        }
    }
}
