<script lang="ts">
  import Icon from 'src/icons/Icon.svelte'

  export let icon = ''
  export let iconClass = ''
  export let size: 'sm'|''|'lg' = ''
  export let label = ''
  export let id = label.replace(/\W/g, '-')
  export let type: 'button'|'submit' = 'button'
  export let onclick: (e: MouseEvent) => void = () => {}

  $: hasLabel = label || $$slots.default
</script>

<button {type} {id} {onclick} {...$$restProps} class="btn {$$props.class ?? 'default'} {size} inline-flex items-center gap-1" class:icon-only={icon && !hasLabel}>
  {#if icon}
    <Icon name={icon} {size} class="{iconClass}"/>
  {/if}
  {#if hasLabel}
    <slot>{label}</slot>
  {/if}
</button>

<style lang="postcss">
  @reference 'src/global.css';

  :global(.btn) {
    @apply cursor-pointer border border-transparent rounded-md text-center inline-flex
    focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500/75
    disabled:opacity-50 justify-center py-2 px-4 text-sm font-medium
    active:opacity-75 active:transition-opacity;
  }

  :global(.btn.default) {
    @apply focus:ring-primary-500 hover:bg-gray-50 bg-white text-gray-700 border border-gray-300;
  }

  :global(.btn.primary) {
    @apply text-white hover:text-white bg-primary-600 hover:bg-primary-700 focus:ring-primary-500;
  }

  :global(.btn.danger) {
    @apply text-danger-500 border border-gray-300 hover:bg-gray-50;
  }

  :global(.btn.success) {
    @apply text-success-500 border-gray-300 bg-white hover:bg-gray-50 focus:ring-success-500;
  }

  :global(.btn.link) {
    @apply text-primary-600 hover:text-primary-500 hover:bg-primary-100 focus:ring-primary-400;
  }

  :global(.btn.sm) {
    @apply py-1.5 px-3;
  }

  :global(.btn.lg) {
    @apply py-3 px-4 font-semibold;
  }

  :global(.btn.icon-only) {
    @apply px-2;
  }

  :global(.btn.icon-only.sm) {
    @apply px-1.5;
  }

  :global(.btn.icon-first) {
    @apply flex-col text-sm items-center whitespace-nowrap;
  }

  :global(.btn.icon-first span) {
    @apply !m-0;
  }
</style>
