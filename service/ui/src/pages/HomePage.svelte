<script lang="ts">
  import api, {setApiKey} from 'src/api/api'
  import type {Envelope} from 'src/api/types'
  import Badge from 'src/components/Badge.svelte'
  import Button from 'src/components/Button.svelte'
  import FileField from 'src/components/FileField.svelte'
  import KeyValue from 'src/components/KeyValue.svelte'
  import Section from 'src/components/Section.svelte'
  import type {Attachment} from 'src/components/types'
  import Form from 'src/forms/Form.svelte'
  import {showToast, ToastType} from 'src/stores/toasts'

  setApiKey(import.meta.env.VITE_PLATFORM_API_KEY ?? 'dev-insecure-key')

  let attachment: Attachment | undefined
  let envelope: Envelope | undefined
  let busy = false

  $: canCreate = !!attachment && !busy

  async function create() {
    if (!attachment) return
    busy = true
    try {
      envelope = await api.createEnvelope(attachment.file)
      showToast(`Request ${envelope.id} created`)
    } catch (e: any) {
      showToast(e.message || 'Request failed', {type: ToastType.ERROR})
    } finally {
      busy = false
    }
  }
</script>

<main class="mx-auto max-w-3xl p-6 spaced">
  <div class="text-center">
    <h1>CMR Sign Test Service</h1>
    <p>This service is used for testing the CMR Sign core functionality through ui.</p>
  </div>

  <Section title="Create signing request">
    <Form submit={create}>
      <FileField bind:value={attachment} label="CMR PDF" multiple={false} required accept="application/pdf"/>
      <Button class="primary" type="submit" label="Create request" disabled={!canCreate}/>
    </Form>
  </Section>

  {#if envelope}
    <Section title="Request">
      <div class="flex items-center gap-4">
        <Badge class="info">{envelope.state}</Badge>
        <KeyValue label="Request id" value={envelope.id}/>
      </div>
    </Section>
  {/if}
</main>
