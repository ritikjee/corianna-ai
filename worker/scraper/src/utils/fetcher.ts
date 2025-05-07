import axios from 'axios'

export const fetcher = async <T>(payload: {
    url: string
    method?: string
    data?: unknown
}) => {
    try {
        const { url, method = 'GET', data } = payload

        const response = await axios({
            method,
            url,
            data,
        })

        return {
            data: response.data as T,
            error: null,
        }
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (error: any) {
        return {
            data: null,
            error: {
                message: error.response?.data.message || 'Something went wrong',
                status: error.response?.status || 500,
            },
        }
    }
}
