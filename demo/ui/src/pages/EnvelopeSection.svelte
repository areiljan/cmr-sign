<script lang="ts">
    import Section from "src/components/Section.svelte";
    import Badge from "src/components/Badge.svelte";
    import KeyValue from "src/components/KeyValue.svelte";
    import Button from "src/components/Button.svelte";
    import CheckboxField from "src/forms/CheckboxField.svelte";
    import {type Envelope, EnvelopeState, type LinkIssued, type SessionStarted, SignerClass, type Signed} from 'src/api/types'
    import api from "src/api/api";
    import {showToast, ToastType} from "src/stores/toasts";

    export let envelope: Envelope
    export let onSigned: (signed: Signed) => void = () => {}

    let documentVersion: number | undefined
    let documentSha256: string | undefined
    let consent = false
    let activationCode = ''
    let link: LinkIssued | undefined

    $: canStart = envelope.state === EnvelopeState.CREATED

    async function start() {
        if (!envelope) return
        try {
            apply(await api.startSession(envelope.id))
        } catch (e: any) {
            showToast(e.message || 'Starting the session failed', {type: ToastType.ERROR})
        }
    }

    async function authorize() {
        try {
            apply(await api.authorizeSession(envelope.id, activationCode))
            activationCode = ''
        } catch (e: any) {
            showToast(e.message || 'Authorization failed', {type: ToastType.ERROR})
        }
    }

    async function createLink() {
        try {
            link = await api.createLink(envelope.id)
        } catch (e: any) {
            showToast(e.message || 'Creating the link failed', {type: ToastType.ERROR})
        }
    }

    function apply(started: SessionStarted) {
        envelope = {
            ...envelope,
            state: started.state,
            providerSessionId: started.providerSessionId,
            signerSubject: started.signerSubject,
            proofingMethod: started.proofingMethod,
        }
        documentVersion = started.documentVersion
        documentSha256 = started.documentSha256
        showToast(started.state === EnvelopeState.AUTHORIZATION_PENDING ? 'The provider needs a one-time code' : 'Identity proven')
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
    {#if envelope.signerClass === SignerClass.AD_HOC}
        {#if link}
            <KeyValue label="Signing link" value={location.origin + link.path} valueClass="font-mono text-xs break-all"/>
            <KeyValue label="One-time code (send by SMS in production)" value={link.otp} valueClass="font-mono"/>
            <KeyValue label="Expires" value={link.expiresAt}/>
            <a class="btn primary" href={link.path} target="_blank">Open as signer</a>
        {:else}
            <Button class="primary" label="Create signing link" onclick={createLink}/>
        {/if}
    {:else if envelope.state === EnvelopeState.CREATED}
        <Button class="primary" label="Start session" onclick={start} disabled={!canStart}/>
    {:else}
        <div class="flex flex-wrap gap-4">
            <KeyValue label="Signer" value={envelope.signerSubject}/>
            <KeyValue label="Proofing" value={envelope.proofingMethod}/>
            <KeyValue label="Document version" value={documentVersion}/>
        </div>
        {#if envelope.state === EnvelopeState.AUTHORIZATION_PENDING}
            <form class="flex items-end gap-2" on:submit|preventDefault={authorize}>
                <label class="form-field">Provider one-time code
                    <input bind:value={activationCode} autocomplete="one-time-code" inputmode="numeric" required>
                </label>
                <Button class="primary" type="submit" label="Authorize"/>
            </form>
        {/if}
        {#if envelope.state === EnvelopeState.IDENTITY_PROVEN}
            <KeyValue label="SHA-256" value={documentSha256} valueClass="font-mono text-xs break-all"/>
            <CheckboxField label="I have reviewed the document and consent to signing this hash"
                           bind:checked={consent}/>
            <Button class="primary" label="Sign" onclick={sign} disabled={!consent}/>
        {/if}
    {/if}
</Section>
