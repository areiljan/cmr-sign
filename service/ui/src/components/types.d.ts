export interface Attachment {
  file: File
  id?: string
  href?: string
}

export interface FileMetadata {
  id?: string
  name: string
  url?: string
}

export interface Step {
  key: string
  title: string
  completed: boolean
  locked: boolean
}
