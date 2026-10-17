<script lang="ts">
  export let title: string | undefined = undefined
</script>

<span class="badge {$$props.class ?? ''}" {title}>
  <slot/>
</span>

<style lang="postcss">
  @reference "src/global.css";

  :global(.badge) {
    @apply inline-flex items-center px-2.5 py-0.5 rounded-md border border-secondary-300 font-medium text-gray-800 bg-primary-100;
  }

  :global(.badge.success) {
    @apply bg-success-100;
  }

  :global(.badge.danger) {
    @apply bg-danger-100;
  }

  :global(.badge.warning) {
    @apply bg-warning-100;
  }

  :global(.badge.info) {
    @apply bg-blue-200;
  }
</style>
