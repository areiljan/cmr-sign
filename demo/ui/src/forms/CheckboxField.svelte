<script lang="ts">
  export let label: string|undefined = undefined
  export let checked = false
  export let helpText = ''

  export let id = label ? label.replace(/\./g, '-') : ''
</script>

<div class="relative flex items-center {$$props.class ?? ''}">
  <div class="flex items-center h-5">
    <input {id} type="checkbox" bind:checked {...$$restProps}>
  </div>
  <label for={id} class="ml-2">
    {#if $$slots.default}
      <slot/>
    {:else}
      {label}
    {/if}
  </label>
  {#if helpText}<span class="help-text ml-1 cursor-default" title={helpText}>ⓘ</span>{/if}
</div>

<style lang="postcss">
  @reference "./../global.css";

  :global(input[type=checkbox]) {
    @apply focus:ring-primary-500 h-4! w-4! text-primary-500 border-gray-300 rounded;
  }
</style>
