package br.com.bruno.orange.pixdesafio.pix.consulta

import br.com.bruno.orange.pixdesafio.ConsultaChavePixRequest
import br.com.bruno.orange.pixdesafio.ConsultaChavePixResponse
import br.com.bruno.orange.pixdesafio.KeymanagerConsultaGrpc
import br.com.bruno.orange.pixdesafio.externo.bcb.ErpBcb
import br.com.bruno.orange.pixdesafio.pix.ChavePixRepository
import br.com.bruno.orange.pixdesafio.validation.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@ErrorHandler
@Singleton
class ConsultaChaveEndpoint(
        @Inject val repository: ChavePixRepository,
        @Inject val erpBcb: ErpBcb,
        @Inject private val validator: Validator): KeymanagerConsultaGrpc.KeymanagerConsultaImplBase() {
        override fun carrega(
                request: ConsultaChavePixRequest?,
                responseObserver: StreamObserver<ConsultaChavePixResponse>?
        ) {
                val filtro = request!!.toModel(validator)
                val chaveInfo = filtro.filtra(repository, erpBcb)
                responseObserver!!.onNext(ConsultaChavePixResponseConverter().convert(chaveInfo))
                responseObserver.onCompleted()
        }
        }