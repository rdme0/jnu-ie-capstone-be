package jnu.ie.capstone.clova.client

import io.grpc.Metadata
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.MetadataUtils
import jnu.ie.capstone.clova.config.ClovaConfig
import org.springframework.stereotype.Component

@Component
class ClovaClient(
    private val config: ClovaConfig
) {
    private val channel = NettyChannelBuilder
        .forTarget(config.speechUrl)
        .useTransportSecurity()
        .build()

    private val metadata = getMetadata()


    suspend fun SpeechToText() {

    }

    private fun getMetadata(): Metadata {
        val metadata = Metadata()
        metadata.put(
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
            "Bearer ${config.apiKey}"
        )

        return metadata
    }
}