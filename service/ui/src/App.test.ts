import {render} from '@testing-library/svelte'
import {activePath} from '@keksworks/svelte-tiny-router'
import App from './App.svelte'

describe('App', () => {
  beforeEach(() => activePath.set('/'))

  it('renders the home page at /', () => {
    const {container} = render(App)

    expect(container.textContent).toContain('CMR Sign Test Service')
  })

  it('renders the not found page for an unknown path', () => {
    activePath.set('/unknown')

    const {container} = render(App)

    expect(container.textContent).toContain('The page you are looking for does not exist.')
  })
})
