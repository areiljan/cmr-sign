export const headers = {'Content-Type': 'application/json; charset=UTF-8', 'Accept': 'application/json'} as HeadersInit

type Body = object | string | FormData | File

class Api {
  prefix = '/api/'

  async get<T>(path: string): Promise<T> {
    return await this.request(path) as T
  }

  async post<T>(path: string, body?: Body): Promise<T> {
    return await this.request(path, {method: 'POST', body}) as T
  }

  private async request(path: string, init?: RequestInit | {body?: Body, headers?: HeadersInit}): Promise<any> {
    if (path.startsWith('/')) throw new Error('Invalid path: ' + path)
    const body = init?.body
    const response = await fetch(this.prefix + path, {
      ...init,
      body: body instanceof File || body instanceof FormData || typeof body == 'string' ? body : body && JSON.stringify(body),
      headers: init?.headers ?? headers
    })
    const data = response.status == 204 ? await response.text() : await response.json()
    if (response.status < 200 || response.status >= 400) {
      throw Object.assign(data, {message: data.message || data.reason})
    }
    return data
  }
}

export default new Api()
