package io.github.areiljan.cmrsign.service.db

import io.github.areiljan.cmrsign.service.envelopes.DocumentRepository
import io.github.areiljan.cmrsign.service.envelopes.EnvelopeRepository
import io.mockk.clearAllMocks
import io.mockk.mockk
import klite.DependencyInjectingRegistry

abstract class BaseMocks {
    companion object {
        val registry = DependencyInjectingRegistry()

        val envelopes = mock<EnvelopeRepository>(relaxed = true)
        val documents = mock<DocumentRepository>(relaxed = true)

        inline fun <reified T : Any> create() = registry.create(T::class)

        inline fun <reified T : Any> mock(relaxed: Boolean = false, crossinline block: T.() -> Unit = {}) =
            mockk<T>(relaxed = relaxed, relaxUnitFun = true, block = block).also { registry.register(T::class, it) }
    }

    init {
        clearAllMocks()
    }
}
