package it.unibo.mobilesystems.actors

import it.unibo.kactor.annotations.HostName
import it.unibo.kactor.annotations.QakContext
import it.unibo.kactor.annotations.Tracing

const val GIT_BERTO_CTX_NAME = "ctxgitberto"
const val GIT_BERTO_CTX_PORT = 9699

@HostName("localhost")
@QakContext(GIT_BERTO_CTX_NAME, "localhost", "TCP", GIT_BERTO_CTX_PORT)
//@Tracing
class ContextConfiguration {
}