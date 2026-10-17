<script lang="ts">
  import {t} from 'src/i18n'
  import type {Attachment} from 'src/components/types'
  import Button from 'src/components/Button.svelte'
  import FileItem from 'src/components/FileItem.svelte'
  import type {FileMetadata} from 'src/components/types'

  export let label = ''
  export let multiple = true
  export let value: Attachment | Attachment[] | undefined = multiple ? [] : undefined
  export let required = false
  export let accept = ''
  export let onchange = () => {}

  let dragging = false

  $: id = label ? label.replace(/\W/g, '-') : 'file-upload'

  $: renderFiles = Array.isArray(value) ? value : (value ? [value] : [])

  function processFiles(rawFiles: File[]) {
    if (!rawFiles.length) return

    if (multiple) {
      const currentFiles = Array.isArray(value) ? value : []
      value = [...currentFiles, ...rawFiles.map(file => ({file}))]
    } else {
      value = {file: rawFiles[0]}
    }
    onchange()
  }

  function handleSelect(event: Event) {
    const target = event.target as HTMLInputElement
    if (target.files) processFiles(Array.from(target.files))
  }

  function handleDrop(event: DragEvent) {
    event.preventDefault()
    dragging = false
    if (event.dataTransfer?.files) processFiles(Array.from(event.dataTransfer.files))
  }

  function handleDragOver(event: DragEvent) {
    event.preventDefault()
    dragging = true
  }

  function handleDragLeave() {
    dragging = false
  }

  function removeFile(attachment: Attachment) {
    if (multiple) {
      const currentFiles = Array.isArray(value) ? value : []
      value = currentFiles.filter(f => f !== attachment)
    } else {
      value = undefined
    }
    onchange()
  }
</script>

<div class="form-field flex flex-col gap-1">
  {#if label}
    <label for={id}>{label}</label>
  {/if}
  <div class="flex flex-wrap gap-2">
    {#each renderFiles as attachment}
      {@const file = {id: attachment.id,  name: attachment.file.name, url: attachment.href} as FileMetadata}
      <FileItem {file}>
        <Button type="button" class="default text-red-500!" title={t.general.remove} icon="x"
                onclick={() => removeFile(attachment)}/>
      </FileItem>
    {/each}

    {#if multiple || renderFiles.length === 0}
      <label
        class="flex-1 min-w-48 flex items-center justify-center p-2 text-center relative border border-gray-300 border-dashed cursor-pointer rounded-lg {dragging ? 'bg-blue-100' : 'bg-gray-50'}"
        ondragover={handleDragOver} ondragleave={handleDragLeave} ondrop={handleDrop}>
        <span class="text-sm text-gray-600 font-medium">{t.shipments.dragFilesHere}</span>
        <input {id} type="file" {multiple} {required} {accept} class="absolute inset-0 opacity-0 pointer-events-none" onchange={handleSelect}>
      </label>
    {/if}
  </div>
</div>

<style lang="postcss">
  @reference "src/global.css";

  :global(.validated) label:has(:invalid) {
    @apply border-danger-500;
  }
</style>
