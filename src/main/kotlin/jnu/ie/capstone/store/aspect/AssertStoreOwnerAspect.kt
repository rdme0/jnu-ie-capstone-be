package jnu.ie.capstone.store.aspect

import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.common.resolver.AspectParameterResolver
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.store.annotation.AssertStoreOwner
import jnu.ie.capstone.store.exception.YouAreNotStoreOwnerException
import jnu.ie.capstone.store.service.StoreService
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.expression.EvaluationContext
import org.springframework.stereotype.Component

@Aspect
@Component
class AssertStoreOwnerAspect(
    private val storeService: StoreService,
    private val resolver: AspectParameterResolver
) {
    private companion object {
        val logger = mu.KotlinLogging.logger {}
    }

    @Before("@annotation(assertStoreOwner)")
    fun checkStoreOwner(joinPoint: JoinPoint, assertStoreOwner: AssertStoreOwner) {
        val context: EvaluationContext = resolver.buildEvaluationContext(joinPoint)

        logger.debug { "checkStoreOwner context: $context" }

        val storeId: Long = resolver
            .resolve(assertStoreOwner.storeId, context, Long::class.java)
            ?: throw InternalServerException(IllegalArgumentException("storeId가 null 입니다."))

        logger.debug { "checkStoreOwner storeId: $storeId" }

        val memberInfo: MemberInfo = resolver
            .resolve(assertStoreOwner.memberInfo, context, MemberInfo::class.java)
            ?: throw InternalServerException(IllegalArgumentException("MemberInfo가 null 입니다."))

        logger.debug { "checkStoreOwner memberInfo: $memberInfo" }

        storeService.getBy(id = storeId, ownerId = memberInfo.id)
            ?: throw YouAreNotStoreOwnerException()
    }

}