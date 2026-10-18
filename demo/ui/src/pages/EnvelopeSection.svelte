<script lang="ts">
    import Section from "src/components/Section.svelte";
    import Badge from "src/components/Badge.svelte";
    import KeyValue from "src/components/KeyValue.svelte";
    import Button from "src/components/Button.svelte";
    import {type Envelope, EnvelopeState} from 'src/api/types'
    import api from "src/api/api";
    import {showToast, ToastType} from "src/stores/toasts";

    export let envelope: Envelope

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
            showToast('Identity proven')
        } catch (e: any) {
            showToast(e.message || 'Starting the session failed', {type: ToastType.ERROR})
        }
    }

    $: canStart = envelope?.state === EnvelopeState.CREATED
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
        </div>
        {#if envelope.state === EnvelopeState.IDENTITY_PROVEN}
            <slot/>
        {/if}
    {/if}
</Section>
