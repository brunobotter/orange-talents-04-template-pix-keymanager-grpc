package br.com.bruno.orange.pixdesafio.pix.lista

import br.com.bruno.orange.pixdesafio.KeymanagerListaGrpc
import br.com.bruno.orange.pixdesafio.ListaChavePixRequest
import br.com.bruno.orange.pixdesafio.ListaChavePixResponse
import br.com.bruno.orange.pixdesafio.pix.ChavePixRepository
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListaChavesEndpoint(@Inject val repository: ChavePixRepository): KeymanagerListaGrpc.KeymanagerListaImplBase() {


    override fun lista(request: ListaChavePixRequest?, responseObserver: StreamObserver<ListaChavePixResponse>?) {
        if (request!!.clienteId.isNullOrBlank())
            throw IllegalArgumentException("Cliente ID não pode ser nulo ou vazio")
        val clienteId =UUID.fromString(request.clienteId)
        val lista = repository.findAllByClienteId(clienteId).map {
            ListaChavePixResponse.ChavePix.newBuilder()
                .setPixId(it.id.toString())
                .setChave(it.chave)
                .setTipoDaChave(it.tipoDaChave)
                .setTipoDaConta(it.tipoDaConta)
                .setCriadaEm(it.criadaEm.let{
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
                .build()
        }
        responseObserver!!.onNext(ListaChavePixResponse.newBuilder() // 1
            .setClienteId(clienteId.toString())
            .addAllChaves(lista)
            .build())
        responseObserver.onCompleted()
    }
}