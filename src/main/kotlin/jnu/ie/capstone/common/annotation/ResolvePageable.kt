package jnu.ie.capstone.common.annotation

import jnu.ie.capstone.common.constant.enums.SortField

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ResolvePageable(val allowed: Array<SortField>)