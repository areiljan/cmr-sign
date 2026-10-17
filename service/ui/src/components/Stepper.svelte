<script lang="ts">
  import Icon from 'src/icons/Icon.svelte'
  import type {Step} from 'src/components/types'

  export let steps: Step[] = []
  export let active = 0
  export let onSelect: (index: number) => void = () => {}

  function select(index: number, locked: boolean) {
    if (!locked) onSelect(index)
  }
</script>

<ol class="flex flex-wrap items-center gap-1 border-b border-gray-300">
  {#each steps as step, index}
    <li class="flex items-center">
      <button
        type="button"
        disabled={step.locked}
        class="relative flex items-center gap-2 rounded-t-md border border-b-0 px-3 pt-2 pb-2.5 font-medium transition-colors duration-150
          focus:outline-none focus-visible:ring-2 focus-visible:ring-tms-logo/50
          {active === index ? '-mb-px z-10 border-gray-300 bg-white text-tms-logo' : 'mb-1 border-gray-300 text-tms/50'}
          {step.locked ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'}"
        onclick={() => select(index, step.locked)}>
        {#if step.completed}
          <Icon name="circle-check" class="size-5 text-success-500"/>
        {:else}
          <span class="flex items-center justify-center size-5 rounded-full border border-current text-xs">{index + 1}</span>
        {/if}
        {step.title}
      </button>
      {#if index < steps.length - 1}
        <Icon name="chevron-right" class="mx-1 size-4 text-gray-300"/>
      {/if}
    </li>
  {/each}
</ol>
