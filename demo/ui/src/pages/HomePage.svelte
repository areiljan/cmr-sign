<script lang="ts">
  import api, {setApiKey} from 'src/api/api'
  import type {Envelope, Signed} from 'src/api/types'
  import {EnvelopeState} from 'src/api/types'
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
  let signed: Signed | undefined
  let busy = false

  $: canCreate = !!attachment && !busy
  $: canStart = envelope?.state === EnvelopeState.CREATED && !busy
  $: canSign = envelope?.state === EnvelopeState.IDENTITY_PROVEN && !busy

  async function create() {
    if (!attachment) return
    busy = true
    try {
      envelope = await api.createEnvelope(attachment.file)
      signed = undefined
      showToast(`Request ${envelope.id} created`)
    } catch (e: any) {
      showToast(e.message || 'Request failed', {type: ToastType.ERROR})
    } finally {
      busy = false
    }
  }

  async function start() {
    if (!envelope) return
    busy = true
    try {
      const started = await api.startSession(envelope.id)
      envelope = {
        ...envelope,
        state: started.state,
        providerSessionId: started.providerSessionId,
        signerSubject: started.signerSubject,
        proofingMethod: started.proofingMethod,
      }
      showToast('Identity proven')
    } catch (e: any) {
      showToast(e.message || 'Starting the session failed', {type: ToastType.ERROR})
    } finally {
      busy = false
    }
  }

  async function sign() {
    if (!envelope) return
    busy = true
    try {
      signed = await api.signEnvelope(envelope.id)
      envelope = {...envelope, state: signed.state}
      showToast(`Signature ${signed.verdict.indication}`)
    } catch (e: any) {
      showToast(e.message || 'Signing failed', {type: ToastType.ERROR})
    } finally {
      busy = false
    }
  }

  function verdictClass(indication: string) {
    if (indication === 'TOTAL_PASSED') return 'success'
    if (indication === 'TOTAL_FAILED') return 'danger'
    return 'warning'
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
      <div class="flex flex-wrap items-center gap-4">
        <Badge class="info">{envelope.state}</Badge>
        <KeyValue label="Request id" value={envelope.id}/>
      </div>
    </Section>

    <Section title="Signer session">
      {#if envelope.state === EnvelopeState.CREATED}
        <Button class="primary" label="Start session" onclick={start} disabled={!canStart}/>
      {:else}
        <div class="flex flex-wrap gap-4">
          <KeyValue label="Signer" value={envelope.signerSubject}/>
          <KeyValue label="Proofing" value={envelope.proofingMethod}/>
        </div>
        {#if envelope.state === EnvelopeState.IDENTITY_PROVEN}
          <Button class="primary" label="Sign" onclick={sign} disabled={!canSign}/>
        {/if}
      {/if}
    </Section>
  {/if}

  {#if signed}
    <Section title="Signature">
      <div class="flex flex-wrap items-center gap-4">
        <Badge class={verdictClass(signed.verdict.indication)}>{signed.verdict.indication}</Badge>
        <KeyValue label="Qualification" value={signed.verdict.qualification}/>
        <KeyValue label="Document version" value={signed.documentVersion}/>
      </div>
      {#if signed.verdict.subIndication}
        <KeyValue label="Reason" value={signed.verdict.subIndication}/>
      {/if}
      <KeyValue label="SHA-256" value={signed.sha256}/>
    </Section>
  {/if}
</main>
