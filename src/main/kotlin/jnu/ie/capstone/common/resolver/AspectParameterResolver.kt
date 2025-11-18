package jnu.ie.capstone.common.resolver

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.expression.EvaluationContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Component
class AspectParameterResolver {

    fun buildEvaluationContext(joinPoint: JoinPoint): EvaluationContext {
        val signature = joinPoint.signature as MethodSignature
        val paramNames = signature.parameterNames
        val args = joinPoint.args

        val context = StandardEvaluationContext()
        for (i in paramNames.indices) {
            context.setVariable(paramNames[i], args[i])
        }
        return context
    }

    fun <T> resolve(expression: String, context: EvaluationContext, targetType: Class<T>): T? {
        val parser = SpelExpressionParser()
        return parser.parseExpression(expression).getValue(context, targetType)
    }

}