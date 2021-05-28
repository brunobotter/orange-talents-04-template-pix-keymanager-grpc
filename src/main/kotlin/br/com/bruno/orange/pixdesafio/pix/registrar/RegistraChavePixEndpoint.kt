package br.com.bruno.orange.pixdesafio.pix.registrar

import br.com.bruno.orange.pixdesafio.KeymanagerRegistraGrpc
import br.com.bruno.orange.pixdesafio.RegistraChavePixRequest
import br.com.bruno.orange.pixdesafio.RegistraChavePixResponse
import br.com.bruno.orange.pixdesafio.validation.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
@ErrorHandler
@Singleton
class RegistraChavePixEndpoint(@Inject private val service: NovaChavePixService) : KeymanagerRegistraGrpc.KeymanagerRegistraImplBase(){

    override fun criar(request: RegistraChavePixRequest?, responseObserver: StreamObserver<RegistraChavePixResponse>?) {
      println(request)
       val novaChave = request!!.toModel()
        val service = service.salvar(novaChave)
        responseObserver?.onNext(RegistraChavePixResponse.newBuilder()
            .setClienteId(service.clienteId.toString())
            .setPixId(service.chave)
            .build())
        responseObserver?.onCompleted()
    }
}