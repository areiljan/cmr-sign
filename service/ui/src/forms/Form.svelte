<script lang="ts">
  import type {FormEventHandler} from 'svelte/elements'

  export let submit: FormEventHandler<HTMLFormElement>
  export let validated = false
  export let formEl: HTMLFormElement | undefined = undefined
</script>

<form {...$$restProps} bind:this={formEl} class={$$props.class ?? 'spaced'}
      class:validated on:invalid|capture={() => validated = true} on:submit|preventDefault={submit}>
  <slot/>
</form>

<style lang="postcss">
  @reference "./../global.css";

  .validated :global(input:invalid), .validated :global(select:invalid), .validated :global(textarea:invalid), .validated :global(.invalid) {
    @apply border-danger-500 ring-danger-500 focus:border-danger-500 focus:ring-danger-500;
  }
</style>
