package br.com.bruno.orange.pixdesafio.pix.registrar

import br.com.bruno.orange.pixdesafio.KeymanagerRegistraGrpc
import br.com.bruno.orange.pixdesafio.RegistraChavePixRequest
import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaConta
import br.com.bruno.orange.pixdesafio.externo.itau.ErpItau
import br.com.bruno.orange.pixdesafio.pix.ContaAssociada
import br.com.bruno.orange.pixdesafio.pix.DadosDaContaResponse
import br.com.bruno.orange.pixdesafio.pix.InstituicaoResponse
import br.com.bruno.orange.pixdesafio.pix.TitularResponse
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
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
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RegistraChavePixEndpointTest(val repository: ChavePixRepository,
val grpcCliente: KeymanagerRegistraGrpc.KeymanagerRegistraBlockingStub){

    @Inject
    lateinit var erpItau: ErpItau

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @MockBean(ErpItau::class)
    fun itauClient(): ErpItau? {
        return Mockito.mock(ErpItau::class.java)
    }

    @Factory
    class Clients  {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeymanagerRegistraGrpc.KeymanagerRegistraBlockingStub? {
            return KeymanagerRegistraGrpc.newBlockingStub(channel)
        }
    }
    //happy-path
    @Test
    fun `consegue registrar no banco uma chave pix`(){
        //cenario
        `when` (erpItau.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))
        //açao
        val response = grpcCliente.criar(
            RegistraChavePixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDaChave(TipoDaChave.EMAIL)
            .setValorDaChave("rponte@gmail.com")
            .setTipoDaConta(TipoDaConta.CONTA_CORRENTE)
            .build())
        //validação
        with(response){
            assertEquals(CLIENTE_ID.toString(), clienteId)
            assertNotNull(pixId)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando nao encontrar dados da conta cliente`(){
        // cenário
        `when`(erpItau.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcCliente.criar(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoDaChave(TipoDaChave.EMAIL)
                    .setValorDaChave("rponte@gmail.com")
                    .setTipoDaConta(TipoDaConta.CONTA_CORRENTE)
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itau", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando parametros forem invalidos`(){
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcCliente.criar(RegistraChavePixRequest.newBuilder().build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)

        }
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {
        return DadosDaContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("UNIBANCO ITAU SA", ContaAssociada.ITAU_UNIBANCO_ISPB),
            agencia = "1218",
            numero = "291900",
            titular = TitularResponse("Rafael Ponte", "63657520325")
        )
    }
}

