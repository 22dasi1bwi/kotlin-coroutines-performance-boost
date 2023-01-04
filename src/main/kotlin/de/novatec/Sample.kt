package de.novatec

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope

class Sample(
    private val systemLoader: SystemLoader,
    private val handler: HttpHandler
) {

    suspend operator fun invoke(): Set<SystemResult> {
        val callsToPerform = supervisorScope {
            val systemsToCall: List<SystemCall> = systemLoader.findAll()
            systemsToCall.map {
                async {
                    handler.performHttpCall(it)
                }
            }
        }
//        This is an alternative of handling failures

//        return callsToPerform.fold(mutableSetOf()) { acc, element ->
//            runCatching { element.await() }
//                .onSuccess { acc.add(it) }
//                .onFailure { println("Can't get result, because of $it.") }
//            acc
//        }
        return callsToPerform.awaitAll().toSet()
    }
}

data class SystemResult(val identifier: String, val data: Map<String, Any>)
data class SystemCall(val identifier: String)
class HttpHandler {
    suspend fun performHttpCall(systemCall: SystemCall): SystemResult {
        delay(1000) // simulating an HTTP call
        return SystemResult(systemCall.identifier, mapOf())
    }
}

class SystemLoader {
    fun findAll() = listOf(SystemCall("system1"), SystemCall("system2"))
}
