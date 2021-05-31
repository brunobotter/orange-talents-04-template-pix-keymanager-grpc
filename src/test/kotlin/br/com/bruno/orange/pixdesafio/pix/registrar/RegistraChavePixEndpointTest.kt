package br.com.bruno.orange.pixdesafio.pix.registrar

import br.com.bruno.orange.pixdesafio.KeymanagerRegistraGrpc
import br.com.bruno.orange.pixdesafio.RegistraChavePixRequest
import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaConta
import br.com.bruno.orange.pixdesafio.externo.bcb.*
import br.com.bruno.orange.pixdesafio.externo.itau.ErpItau
import br.com.bruno.orange.pixdesafio.pix.*
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RegistraChavePixEndpointTest(val repository: ChavePixRepository,
                                            val grpcCliente: KeymanagerRegistraGrpc.KeymanagerRegistraBlockingStub){

    @Inject
    lateinit var erpItau: ErpItau

    @Inject
    lateinit var erpBcb: ErpBcb

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

    @MockBean(ErpBcb::class)
    fun erpBcb(): ErpBcb? {
        return Mockito.mock(ErpBcb::class.java)
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
        `when`(erpBcb.create(createPixKeyRequest()))
            .thenReturn(HttpResponse.created(createPixKeyResponse()))
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
    fun `nao deve registrar chave pix quando nao for possivel registrar chave no BCB`() {
        // cenário
        `when`(erpItau.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(erpBcb.create(createPixKeyRequest()))
            .thenReturn(HttpResponse.badRequest())

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcCliente.criar(RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoDaChave(TipoDaChave.EMAIL)
                .setValorDaChave("rponte@gmail.com")
                .setTipoDaConta(TipoDaConta.CONTA_CORRENTE)
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)", status.description)
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

    private fun createPixKeyRequest(): CreatePixKeyRequest {
        return CreatePixKeyRequest(
            keyType = PixKeyType.EMAIL,
            key = "rponte@gmail.com",
            bankAccount = bankAccount(),
            owner = owner()
        )
    }

    private fun createPixKeyResponse(): CreatePixKeyResponse {
        return CreatePixKeyResponse(
            keyType = PixKeyType.EMAIL,
            key = "rponte@gmail.com",
            bankAccount = bankAccount(),
            owner = owner(),
            createdAt = LocalDateTime.now()
        )
    }

    private fun bankAccount(): BankAccount {
        return BankAccount(
            participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
            branch = "1218",
            accountNumber = "291900",
            accountType = BankAccount.AccountType.CACC
        )
    }

    private fun owner(): Owner {
        return Owner(
            type = Owner.OwnerType.NATURAL_PERSON,
            name = "Rafael Ponte",
            taxIdNumber = "63657520325"
        )
    }
}

