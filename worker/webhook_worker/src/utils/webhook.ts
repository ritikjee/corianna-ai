import axios, { AxiosError } from 'axios'

interface WebhookResponse<T = unknown> {
    success: boolean
    status: number
    message: string
    data: T | null
}

export const sendWebhook = async (
    url: string,
    data: unknown,
    headers: Record<string, string> = {}
): Promise<WebhookResponse> => {
    try {
        const response = await axios.post(url, data, {
            headers: {
                'Content-Type': 'application/json',
                ...headers,
            },
        })

        return {
            success: true,
            status: response.status,
            message: 'Webhook sent successfully',
            data: response.data,
        }
    } catch (error) {
        const axiosError = error as AxiosError

        return {
            success: false,
            status: axiosError.response?.status || 500,
            message: axiosError.message || 'Unknown error occurred',
            data: axiosError.response?.data || null,
        }
    }
}
