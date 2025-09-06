package jnu.ie.capstone.clova.config

import jnu.ie.capstone.grpc.NestServiceGrpc
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.stereotype.Component

@Component
class ClovaGrpcConfig(
    @param:Value("\${spring.grpc.client.default-channel.address}")
    private val grpcTarget: String
) {

    @Bean
    fun stub(channels: GrpcChannelFactory): NestServiceGrpc.NestServiceStub {
        return NestServiceGrpc.newStub(channels.createChannel(grpcTarget))
    }
}