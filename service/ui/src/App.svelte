<script lang="ts">
    import {Route, Router} from '@keksworks/svelte-tiny-router'
    import HomePage from './pages/HomePage.svelte'
    import NotFoundPage from './pages/NotFoundPage.svelte'
</script>

<svelte:head>
    <title>CMR Sign</title>
</svelte:head>

<Router noMatch={NotFoundPage}>
    <Route path="/" component={HomePage}/>
</Router>
