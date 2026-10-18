<script lang="ts">
    import Section from "src/components/Section.svelte";
    import Badge from "src/components/Badge.svelte";
    import KeyValue from "src/components/KeyValue.svelte";
    import Button from "src/components/Button.svelte";
    import CheckboxField from "src/forms/CheckboxField.svelte";
    import {type Envelope, EnvelopeState, type Signed} from 'src/api/types'
    import api from "src/api/api";
    import {showToast, ToastType} from "src/stores/toasts";

    export let envelope: Envelope
    export let onSigned: (signed: Signed) => void = () => {}

    let documentVersion: number | undefined
    let documentSha256: string | undefined
    let consent = false

    $: canStart = envelope.state === EnvelopeState.CREATED

    async function start() {
        if (!envelope) return
        try {
            const started = await api.startSession(envelope.id)
            envelope = {
                ...envelope,
                state: started.state,
                providerSessionId: started.providerSessionId,
                signerSubject: started.signerSubject,
                proofingMethod: started.proofingMethod,
            }
            documentVersion = started.documentVersion
            documentSha256 = started.documentSha256
            showToast('Identity proven')
        } catch (e: any) {
            showToast(e.message || 'Starting the session failed', {type: ToastType.ERROR})
        }
    }

    async function sign() {
        if (!documentSha256) return
        try {
            const signed = await api.signEnvelope(envelope.id, documentSha256)
            console.log(signed)
            envelope = {...envelope, state: signed.state}
            onSigned(signed)
            showToast(`Signature ${signed.verdict.indication}`)
        } catch (e: any) {
            showToast(e.message || 'Signing failed', {type: ToastType.ERROR})
        }
    }
</script>

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
            <KeyValue label="Document version" value={documentVersion}/>
        </div>
        {#if envelope.state === EnvelopeState.IDENTITY_PROVEN}
            <KeyValue label="SHA-256" value={documentSha256} valueClass="font-mono text-xs break-all"/>
            <CheckboxField label="I have reviewed the document and consent to signing this hash"
                           bind:checked={consent}/>
            <Button class="primary" label="Sign" onclick={sign}/>
        {/if}
    {/if}
</Section>
