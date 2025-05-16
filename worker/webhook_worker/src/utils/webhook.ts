import axios from 'axios'

export const sendWebhook = async (
    url: string,
    data: unknown,
    header: Record<string, string>
) => {
    try {
        const response = await axios.post(url, data, {
            headers: {
                'Content-Type': 'application/json',
                ...header,
            },
        })
        return response
    } catch (error) {
        console.error('Error sending webhook:', error)
        return error
    }
}
