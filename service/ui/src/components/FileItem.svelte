<script lang="ts">
  import Icon from 'src/icons/Icon.svelte'
  import type {FileMetadata} from 'src/components/types'

  export let file: FileMetadata
</script>

<div class="flex items-center gap-2 p-1 border border-gray-300 rounded-lg hover:bg-gray-50">
  <Icon name="file" class="size-8! text-gray-300"/>
  <a href={file.url} target="_blank" class="flex items-start justify-start gap-4">
    <span class="font-medium hover:underline text-black text-sm">{file.name}</span>
  </a>
  <slot/>
</div>
