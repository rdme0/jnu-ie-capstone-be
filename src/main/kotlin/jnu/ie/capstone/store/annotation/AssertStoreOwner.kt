package jnu.ie.capstone.store.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AssertStoreOwner(
    val memberInfo: String,
    val storeId: String
)