import {mount} from 'svelte'
import './global.css'
import App from './App.svelte'

mount(App, {target: document.getElementById('app')!})
