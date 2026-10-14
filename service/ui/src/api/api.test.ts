import api, {headers} from './api'
import type {MockInstance} from 'vitest'

const okResponse = (data: unknown) => ({status: 200, json: () => data}) as any

describe('api', () => {
  let fetchSpy: MockInstance

  beforeEach(() => {
    fetchSpy = vi.spyOn(window, 'fetch')
  })

  it('gets json from under /api', async () => {
    fetchSpy.mockResolvedValue(okResponse({id: 'E-1'}))

    const result = await api.get('envelopes/E-1')

    expect(fetchSpy).toHaveBeenCalledWith('/api/envelopes/E-1', {headers})
    expect(result).toEqual({id: 'E-1'})
  })

  it('serialises a json post body', async () => {
    fetchSpy.mockResolvedValue(okResponse({id: 'E-1'}))

    await api.post('envelopes', {format: 'PADES'})

    expect(fetchSpy).toHaveBeenCalledWith('/api/envelopes', {
      method: 'POST',
      body: '{"format":"PADES"}',
      headers,
    })
  })

  it('rejects with the response body on http errors', async () => {
    fetchSpy.mockResolvedValue({status: 403, json: () => ({reason: 'Forbidden'})} as any)

    await expect(api.get('envelopes')).rejects.toMatchObject({message: 'Forbidden'})
  })

  it('rejects paths starting with a slash', async () => {
    await expect(api.get('/envelopes')).rejects.toThrow('Invalid path: /envelopes')
  })
})
