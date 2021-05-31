package br.com.bruno.orange.pixdesafio.pix.deletar

import br.com.bruno.orange.pixdesafio.DeletaChavePixRequest
import br.com.bruno.orange.pixdesafio.DeletaChavePixResponse
import br.com.bruno.orange.pixdesafio.KeymanagerDeletaGrpc
import br.com.bruno.orange.pixdesafio.validation.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class DeletaChavePixEndpoint(@Inject val service: DeletaChavePixService): KeymanagerDeletaGrpc.KeymanagerDeletaImplBase() {

    override fun deletar(request: DeletaChavePixRequest?, responseObserver: StreamObserver<DeletaChavePixResponse>?) {

        service.deletar(request!!.clienteId, request.pixId)
        responseObserver!!.onNext(DeletaChavePixResponse.newBuilder()
            .setClienteId(request.clienteId)
            .setPixId(request.pixId)
            .build())
        responseObserver.onCompleted()
    }
}