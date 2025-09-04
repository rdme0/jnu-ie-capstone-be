package jnu.ie.capstone.clova.interceptor

import io.grpc.*
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import jnu.ie.capstone.clova.config.ClovaConfig
import org.springframework.stereotype.Component

@Component
class ClovaGrpcInterceptor(private val config: ClovaConfig) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>, callOptions: CallOptions, next: Channel
    ): ClientCall<ReqT?, RespT?> {
        return object :
            SimpleForwardingClientCall<ReqT?, RespT?>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT?>?, headers: Metadata) {
                headers.put(
                    Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                    "Bearer ${config.apiKey}"
                )
                super.start(responseListener, headers)
            }
        }
    }

}