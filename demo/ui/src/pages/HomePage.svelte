<script lang="ts">
  import api, {setApiKey} from 'src/api/api'
  import type {Envelope, Signed} from 'src/api/types'
  import Button from 'src/components/Button.svelte'
  import FileField from 'src/components/FileField.svelte'
  import Section from 'src/components/Section.svelte'
  import type {Attachment} from 'src/components/types'
  import Form from 'src/forms/Form.svelte'
  import {showToast, ToastType} from 'src/stores/toasts'
  import EnvelopeSection from "src/pages/EnvelopeSection.svelte";
  import SignatureSection from "src/pages/SignatureSection.svelte";

  setApiKey(import.meta.env.VITE_PLATFORM_API_KEY ?? 'dev-insecure-key')

  let attachment: Attachment | undefined
  let envelope: Envelope | undefined
  let signed: Signed | undefined

  $: canCreate = !!attachment

  async function create() {
    if (!attachment) return
    try {
      envelope = await api.createEnvelope(attachment.file)
      signed = undefined
      showToast(`Request ${envelope.id} created`)
    } catch (e: any) {
      showToast(e.message || 'Request failed', {type: ToastType.ERROR})
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
    <EnvelopeSection bind:envelope onSigned={result => signed = result}/>
  {/if}

  {#if signed}
    <SignatureSection {signed}/>
  {/if}
</main>
