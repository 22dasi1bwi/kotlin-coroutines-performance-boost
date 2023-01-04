package de.novatec

import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SampleTest {

    private val system1 = SystemCall("system1")

    private val system2 = SystemCall("system2")

    private val systemLoader = mockk<SystemLoader>().also {
        every { it.findAll() } returns listOf(system1, system2)
    }

    private val handler = mockk<HttpHandler>()

    private val cut = Sample(systemLoader, handler)

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `total runtime is determined by slowest response`() = runTest {
        val virtualStartTime = currentTime
        coEvery { handler.performHttpCall(any()) } coAnswers {
            val systemCall = firstArg<SystemCall>()
            if (systemCall == system1) delay(2000) else delay(5000)

            SystemResult(systemCall.identifier, mapOf())
        }

        cut.invoke()

        val totalTime = currentTime - virtualStartTime
        totalTime shouldBe 5000
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `is not disrupted by unsuccessful calls`() = runTest {
        val expectedException = IllegalArgumentException("unexpected error")
        coEvery { handler.performHttpCall(system1) } throws expectedException
        coEvery { handler.performHttpCall(system2) } returns SystemResult(system2.identifier, mapOf())

        runCatching { cut.invoke() }
            .onFailure {
                it shouldBe expectedException
                coVerifyAll {
                    handler.performHttpCall(system1)
                    handler.performHttpCall(system2)
                }
            }

    }
}
