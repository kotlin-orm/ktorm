package org.ktorm.ksp.annotation

import org.ktorm.schema.BaseTable
import kotlin.reflect.KClass

/**
 * Be used on Entity interface, to specify the super table class for the table class generated.
 * @property value the super table class.
 * if not specified, the super table class will be determined by the kind of the entity class.
 * `org.ktorm.schema.Table` for interface, `org.ktorm.schema.BaseTable` for class.
 *
 * if there are multiple `SuperTableClass` on the inheritance hierarchy of entity,
 * and they have an inheritance relationship, the super table class will be the last one of them.
 * and they don't have an inheritance relationship, an error will be reported.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class SuperTableClass(
    public val value: KClass<out BaseTable<out Any>>
)
