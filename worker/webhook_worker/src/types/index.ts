export type Webhook = {
    id: string
    name: string
    url: string
    token: string
    headers: Record<string, string>
    isActive: boolean
    createdAt: string
    updatedAt: string
}
