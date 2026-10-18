<script lang="ts">
    import Section from "src/components/Section.svelte"
    import Badge from "src/components/Badge.svelte"
    import KeyValue from "src/components/KeyValue.svelte"
    import type {Signed} from "src/api/types";

    export let signed: Signed

    function verdictClass(indication: string) {
        if (indication === 'TOTAL_PASSED') return 'success'
        if (indication === 'TOTAL_FAILED') return 'danger'
        return 'warning'
    }
</script>
<Section title="Signature">
    <div class="flex flex-wrap items-center gap-4">
        <Badge class={verdictClass(signed.verdict.indication)}>{signed.verdict.indication}</Badge>
        <KeyValue label="Qualification" value={signed.verdict.qualification}/>
        <KeyValue label="Document version" value={signed.documentVersion}></KeyValue>
    </div>
    {#if signed.verdict.subIndication}
        <KeyValue label="Reason" value={signed.verdict.subIndication}/>
    {/if}
    <KeyValue label="SHA-256" value={signed.sha256}/>
</Section>
