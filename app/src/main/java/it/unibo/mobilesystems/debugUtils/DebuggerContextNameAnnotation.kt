package it.unibo.mobilesystems.debugUtils

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class DebuggerContextNameAnnotation(val contextName : String)
