import type {Envelope, SessionStarted, Signed} from './types'
import {SignatureFormat, SignerClass} from './types'

export const headers = {'Content-Type': 'application/json; charset=UTF-8', 'Accept': 'application/json'} as HeadersInit

let apiKey = ''

export function setApiKey(key: string) {
    apiKey = key
}

function requestHeaders(): HeadersInit {
    return {...headers, ...(apiKey ? {'X-Api-Key': apiKey} : {})}
}

type Body = object | string | FormData | File

class Api {
    prefix = '/api/'

    async get<T>(path: string): Promise<T> {
        return await this.request(path) as T
    }

    async post<T>(path: string, body?: Body): Promise<T> {
        return await this.request(path, {method: 'POST', body}) as T
    }

    async createEnvelope(file: File): Promise<Envelope> {
        return this.post('envelopes', {
            contentType: 'application/pdf',
            format: SignatureFormat.PADES,
            signerClass: SignerClass.REGISTERED,
            payload: await toBase64(file),
        })
    }

    startSession(id: string): Promise<SessionStarted> {
        return this.post(`envelopes/${id}/start`)
    }

    signEnvelope(id: string, documentSha256: string): Promise<Signed> {
        return this.post(`envelopes/${id}/sign`, {documentSha256})
    }

    private async request(path: string, init?: RequestInit | { body?: Body, headers?: HeadersInit }): Promise<any> {
        if (path.startsWith('/')) throw new Error('Invalid path: ' + path)
        const body = init?.body
        const response = await fetch(this.prefix + path, {
            ...init,
            body: body instanceof File || body instanceof FormData || typeof body == 'string' ? body : body && JSON.stringify(body),
            headers: init?.headers ?? requestHeaders()
        })
        const data = response.status == 204 ? await response.text() : await response.json()
        if (response.status < 200 || response.status >= 400) {
            throw Object.assign(data, {message: data.message || data.reason})
        }
        return data
    }
}

async function toBase64(file: File): Promise<string> {
    const bytes = new Uint8Array(await file.arrayBuffer())
    let binary = ''
    const chunkSize = 0x8000
    for (let i = 0; i < bytes.length; i += chunkSize) {
        binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize))
    }
    return btoa(binary)
}

export default new Api()
