import { CHROMA_DB_METADATA } from '../types'

export function formatWebsiteContent(data: CHROMA_DB_METADATA[][]): string {
    let output = ''

    data.forEach((sections, pageIndex) => {
        sections.forEach((section) => {
            output += `--- Page ${pageIndex + 1} | Section ${section.sectionNo} ---\n`
            output += `Title: ${section.title}\n`
            output += `URL: ${section.url}\n`
            output += `Content:\n${section.body.trim()}\n\n`
        })
    })

    return output.trim()
}
