<script lang="ts">
  import api, {setApiKey} from 'src/api/api'
  import {EftiDataElement, type Envelope, SignerClass, type Signed} from 'src/api/types'
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
  let signerClass = SignerClass.REGISTERED
  let signerName = ''
  let eftiRole = EftiDataElement.EFTI65_SENDER
  let envelope: Envelope | undefined
  let signed: Signed | undefined

  $: canCreate = !!attachment && (signerClass !== SignerClass.AD_HOC || !!signerName.trim())

  async function create() {
    if (!attachment) return
    try {
      envelope = await api.createEnvelope(attachment.file, signerClass, signerName.trim(), eftiRole)
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
      <div class="grid gap-4 sm:grid-cols-3">
        <label class="form-field">Signer class
          <select bind:value={signerClass}>
            {#each Object.values(SignerClass) as value}<option {value}>{value}</option>{/each}
          </select>
        </label>
        <label class="form-field">eFTI role
          <select bind:value={eftiRole}>
            {#each Object.values(EftiDataElement) as value}<option {value}>{value}</option>{/each}
          </select>
        </label>
        <label class="form-field">Expected signer
          <input bind:value={signerName} placeholder="Mari Maasikas" required={signerClass === SignerClass.AD_HOC}>
        </label>
      </div>
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
