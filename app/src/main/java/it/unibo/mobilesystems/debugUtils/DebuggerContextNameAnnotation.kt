package it.unibo.mobilesystems.debugUtils


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class DebuggerContextNameAnnotation(val contextName : String)
