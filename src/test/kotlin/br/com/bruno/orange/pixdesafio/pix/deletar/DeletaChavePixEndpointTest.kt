package br.com.bruno.orange.pixdesafio.pix.deletar

import br.com.bruno.orange.pixdesafio.DeletaChavePixRequest
import br.com.bruno.orange.pixdesafio.KeymanagerDeletaGrpc
import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaConta
import br.com.bruno.orange.pixdesafio.externo.bcb.DeletePixKeyRequest
import br.com.bruno.orange.pixdesafio.externo.bcb.DeletePixKeyResponse
import br.com.bruno.orange.pixdesafio.externo.bcb.ErpBcb
import br.com.bruno.orange.pixdesafio.pix.ChavePix
import br.com.bruno.orange.pixdesafio.pix.ChavePixRepository
import br.com.bruno.orange.pixdesafio.pix.ContaAssociada
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class DeletaChavePixEndpointTest(
    val repository: ChavePixRepository,
    val grpcCliente: KeymanagerDeletaGrpc.KeymanagerDeletaBlockingStub
){

    lateinit var CHAVE_EXISTENTE: ChavePix

    @Inject
    lateinit var erpBcb: ErpBcb
    @BeforeEach
    fun setup(){
        CHAVE_EXISTENTE = repository.save(chave(
            tipoDaChave = TipoDaChave.EMAIL,
            chave = "rponte@gmail.com",
            clienteId = UUID.randomUUID()
        ))
    }

    @MockBean(ErpBcb::class)
    fun erpBcb(): ErpBcb? {
        return Mockito.mock(ErpBcb::class.java)
    }

    //happy path
    @Test
    fun `deleta chave pix do banco de dados`(){
        // cenário
        Mockito.`when`(erpBcb.delete("rponte@gmail.com", DeletePixKeyRequest("rponte@gmail.com")))
            .thenReturn(HttpResponse.ok(DeletePixKeyResponse(key = "rponte@gmail.com",
                participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
                deletedAt = LocalDateTime.now()))
            )
       //ação
        val response = grpcCliente.deletar(DeletaChavePixRequest.newBuilder()
            .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
            .setPixId(CHAVE_EXISTENTE.id.toString())
            .build())
        with(response){
            assertEquals(CHAVE_EXISTENTE.clienteId.toString(), clienteId)
            assertEquals(CHAVE_EXISTENTE.id.toString(), pixId)
        }
    }

    @Test
    fun `nao deve remover chave pix quando chave inexistente`() {
        val chavePixDeferente = UUID.randomUUID()
        // ação
        val thrown = org.junit.jupiter.api.assertThrows<StatusRuntimeException> {
            grpcCliente.deletar(
                DeletaChavePixRequest.newBuilder()
                    .setPixId(chavePixDeferente.toString())
                    .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave pix nao cadastrada ou nao pertence ao cliente", status.description)
        }
    }
    @Test
    fun `nao deve remover chave pix existente quando ocorrer algum erro no serviço do BCB`() {
        // cenário
        Mockito.`when`(erpBcb.delete("rponte@gmail.com", DeletePixKeyRequest("rponte@gmail.com")))
            .thenReturn(HttpResponse.unprocessableEntity())

        // ação
        val thrown = org.junit.jupiter.api.assertThrows<StatusRuntimeException> {
            grpcCliente.deletar(DeletaChavePixRequest.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id.toString())
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao remover chave pix do banco central do brasil (BCB)", status.description)
        }
    }

    @Factory
    class Clients  {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeymanagerDeletaGrpc.KeymanagerDeletaBlockingStub? {
            return KeymanagerDeletaGrpc.newBlockingStub(channel)
        }
    }


    private fun chave(
        tipoDaChave: TipoDaChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID(),
    ): ChavePix {
        return ChavePix(
            clienteId = clienteId,
            tipoDaChave = tipoDaChave,
            chave = chave,
            tipoDaConta = TipoDaConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "UNIBANCO ITAU",
                nomeDoTitular = "Rafael Ponte",
                cpfDoTitular = "12345678900",
                agencia = "1218",
                numeroDaConta = "123456"
            )
        )
    }
}