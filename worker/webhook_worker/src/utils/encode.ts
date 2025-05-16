import crypto from 'crypto'

export function encodeValueWithHMAC(value: unknown, secret: string): string {
    const jsonString = JSON.stringify(value)
    return crypto.createHmac('sha256', secret).update(jsonString).digest('hex')
}
