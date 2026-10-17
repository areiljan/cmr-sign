<script lang="ts">
  import Icon from 'src/icons/Icon.svelte'
  import Button from 'src/components/Button.svelte'

  export let title = ''
  export let size: 'xs' | 'sm' | '' | 'lg' = ''
  export let bodyClass = ''
  export let collapse = false

  function toggleCollapse(e: Event) {
    e.stopPropagation()
    collapse = !collapse
  }

  function onKeyDown(e: KeyboardEvent) {
    if (e.key === ' ') {
      e.preventDefault()
      toggleCollapse(e)
    }
  }
</script>

<section class="section {size} {$$props.class ?? ''}" class:collapsed={collapse} oninvalidcapture={() => collapse = false}>
  {#if title}
    <div
      class="section-header"
      onclick={toggleCollapse}
      onkeydown={onKeyDown}
      role="button"
      tabindex="0"
      aria-expanded={!collapse}
    >
      <div class="me-auto">
        <div class="section-title">{title}</div>
      </div>
      <div class="-my-2 ms-1 -me-2 flex items-center gap-1">
        <slot name="actions"/>
        <Button class="circular" size="sm" onclick={toggleCollapse}>
          <Icon name={collapse ? 'chevron-down' : 'chevron-up'}/>
        </Button>
      </div>
    </div>
  {/if}

  <div class="section-body {bodyClass}" class:hidden={collapse}>
    <slot/>
  </div>
</section>

<style lang="postcss">
  @reference "./../global.css";

  section {
    @apply bg-white;
  }

  .section-header {
    @apply sticky top-0 z-10 flex cursor-pointer flex-wrap items-center justify-between border-t border-neutral-200 bg-neutral-100 px-4 py-3 select-none sm:flex-nowrap;
  }

  .section-title {
    @apply text-xs font-semibold uppercase;
  }

  .section-body {
    @apply grow bg-neutral-50 px-4 pt-3 pb-3;
  }

  .section-body:empty {
    @apply hidden;
  }

  section.xs {
    .section-title {
      @apply text-xs font-semibold;
    }

    .section-header,
    .section-body {
      @apply px-2;
    }

    .section-header {
      @apply py-1;
    }

    .section-body {
      @apply py-1;
    }
  }

  section.sm {
    .section-title {
      @apply text-sm font-semibold;
    }

    .section-header,
    .section-body {
      @apply px-3;
    }

    .section-header {
      @apply py-2;
    }

    .section-body {
      @apply py-3;
    }
  }

  section.lg {
    .section-header,
    .section-body {
      @apply px-5;
    }

    .section-header {
      @apply py-4;
    }

    .section-body {
      @apply py-4;
    }
  }
</style>
