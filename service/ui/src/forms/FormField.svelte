<script lang="ts">
  import {t} from 'src/i18n'

  export let label: string|undefined = undefined
  export let showRequiredLabel = true
  export let helpText = ''
  export let value: string|number = ''
  export let validator: ((value: string) => string)|undefined = undefined
  export let id = label?.replace(/\W/g, '-')

  export let minlength = 0
  export let maxlength = 100
  export let required = true

  let input: HTMLInputElement

  $: if (input && validator) input.setCustomValidity(validator(value as string))
</script>

<div class="form-field {$$props.class}">
  {#if label}
    <label for={id}>
      {label}
      {#if helpText}<span class="help-text" title={helpText}>ⓘ</span>{/if}
      {#if !required && showRequiredLabel}<span class="text-muted text-xs">({t.general.optional})</span>{/if}
    </label>
  {/if}
  <slot {id}>
    <div class="flex relative">
      <input {id} bind:this={input} bind:value
        {minlength} {maxlength} {required} {...$$restProps}>
      <slot name="after"/>
    </div>
  </slot>
</div>
