const modules = import.meta.glob('./*.svg', {eager: true, query: '?raw', import: 'default'}) as Record<string, string>

const icons: Record<string, string> = Object.fromEntries(
  Object.entries(modules).map(([name, content]) => [name.replace('./', '').replace('.svg', ''), content]),
)

export default icons
