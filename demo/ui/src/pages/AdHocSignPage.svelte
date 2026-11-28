<script lang="ts">
    import {onMount} from 'svelte'
    import api from 'src/api/api'
    import {EnvelopeState, type LinkView, type Signed} from 'src/api/types'
    import Section from 'src/components/Section.svelte'
    import KeyValue from 'src/components/KeyValue.svelte'
    import Badge from 'src/components/Badge.svelte'
    import Button from 'src/components/Button.svelte'
    import CheckboxField from 'src/forms/CheckboxField.svelte'
    import SignatureSection from 'src/pages/SignatureSection.svelte'
    import {showToast, ToastType} from 'src/stores/toasts'

    export let token: string

    let link: LinkView | undefined
    let error = ''
    let state: EnvelopeState | undefined
    let signer: string | undefined
    let otp = ''
    let activationCode = ''
    let consent = false
    let signed: Signed | undefined

    onMount(async () => {
        try {
            link = await api.link(token)
            state = link.verified ? link.state : undefined
        } catch (e: any) {
            error = e.message || 'This link cannot be used'
        }
    })

    async function verify() {
        try {
            const started = await api.verifyLink(token, otp)
            state = started.state
            signer = started.signerSubject
        } catch (e: any) {
            showToast(e.message || 'Wrong code', {type: ToastType.ERROR})
        }
    }

    async function authorize() {
        try {
            const started = await api.authorizeLink(token, activationCode)
            state = started.state
            signer = started.signerSubject
        } catch (e: any) {
            showToast(e.message || 'Authorization failed', {type: ToastType.ERROR})
        }
    }

    async function sign() {
        if (!link) return
        try {
            signed = await api.signLink(token, link.documentSha256)
            state = signed.state
        } catch (e: any) {
            showToast(e.message || 'Signing failed', {type: ToastType.ERROR})
        }
    }
</script>

<main class="mx-auto max-w-3xl p-6 spaced">
    <h1 class="text-center">Sign a consignment note</h1>

    {#if error}
        <Section title="Link"><p>{error}</p></Section>
    {:else if link}
        <Section title="Document">
            <div class="flex flex-wrap items-center gap-4">
                <Badge class="info">{state ?? 'NOT_VERIFIED'}</Badge>
                <KeyValue label="Signer" value={link.signerName}/>
                <KeyValue label="Link expires" value={link.expiresAt}/>
            </div>
            <KeyValue label="SHA-256" value={link.documentSha256} valueClass="font-mono text-xs break-all"/>
            <a class="btn default" href="/api/links/{token}/document" target="_blank">Open the document</a>
        </Section>

        {#if !state}
            <Section title="Confirm the code you received">
                <form class="flex items-end gap-2" on:submit|preventDefault={verify}>
                    <label class="form-field">One-time code
                        <input bind:value={otp} autocomplete="one-time-code" inputmode="numeric" required>
                    </label>
                    <Button class="primary" type="submit" label="Continue"/>
                </form>
            </Section>
        {:else if state === EnvelopeState.AUTHORIZATION_PENDING}
            <Section title="Authorize the signature at your provider">
                <form class="flex items-end gap-2" on:submit|preventDefault={authorize}>
                    <label class="form-field">Provider one-time code
                        <input bind:value={activationCode} autocomplete="one-time-code" inputmode="numeric" required>
                    </label>
                    <Button class="primary" type="submit" label="Authorize"/>
                </form>
            </Section>
        {:else if state === EnvelopeState.IDENTITY_PROVEN}
            <Section title="Sign">
                <KeyValue label="Certificate" value={signer}/>
                <CheckboxField label="I have reviewed the document and consent to signing this hash" bind:checked={consent}/>
                <Button class="primary" label="Sign" onclick={sign} disabled={!consent}/>
            </Section>
        {/if}

        {#if signed}
            <SignatureSection {signed}/>
        {/if}
    {/if}
</main>
