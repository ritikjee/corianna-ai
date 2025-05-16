import axios from 'axios'

export const fetcher = async <T>(payload: {
    url: string
    method?: string
    data?: Record<string, unknown>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    params?: any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    responseType?: any
}) => {
    try {
        const { url, method = 'GET', data, params, responseType } = payload

        const response = await axios({
            method,
            url,
            data,
            params,
            headers: {
                'Content-Type': 'application/json',
                'x-app-secret': `${process.env.APP_SECRET}`,
            },
            responseType,
        })

        return {
            data: response.data?.data as T,
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
