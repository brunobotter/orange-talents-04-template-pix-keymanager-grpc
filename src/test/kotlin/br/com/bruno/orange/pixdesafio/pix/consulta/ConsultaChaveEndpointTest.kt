package br.com.bruno.orange.pixdesafio.pix.consulta

import br.com.bruno.orange.pixdesafio.ConsultaChavePixRequest
import br.com.bruno.orange.pixdesafio.ConsultaChavePixRequest.FiltroPorPixId
import br.com.bruno.orange.pixdesafio.KeymanagerConsultaGrpc
import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaChave.*
import br.com.bruno.orange.pixdesafio.TipoDaConta.*
import br.com.bruno.orange.pixdesafio.externo.bcb.*
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
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class ConsultaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeymanagerConsultaGrpc.KeymanagerConsultaBlockingStub,
) {

    @Inject
    lateinit var erpBcb: ErpBcb

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    /**
     * TIP: por padrão roda numa transação isolada
     */
    @BeforeEach
    fun setup() {
        repository.save(chave(tipo = EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = CPF, chave = "63657520325", clienteId = UUID.randomUUID()))
        repository.save(chave(tipo = ALEATORIA, chave = "randomkey-3", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = CELULAR, chave = "+551155554321", clienteId = CLIENTE_ID))
    }

    /**
     * TIP: por padrão roda numa transação isolada
     */
    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve carregar chave por pixId e clienteId`() {
        // cenário
        val chaveExistente = repository.findByChave("+551155554321").get()

        // ação
        val response = grpcClient.carrega(ConsultaChavePixRequest.newBuilder()
            .setPixId(
                FiltroPorPixId.newBuilder()
                    .setPixId(chaveExistente.id.toString())
                    .setClienteId(chaveExistente.clienteId.toString())
                    .build()
            ).build())

        // validação
        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipoDaChave.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando filtro invalido`() {
        // ação
        val thrown = org.junit.jupiter.api.assertThrows<StatusRuntimeException> {
            grpcClient.carrega(ConsultaChavePixRequest.newBuilder()
                .setPixId(
                    FiltroPorPixId.newBuilder()
                        .setPixId("")
                        .setClienteId("")
                        .build()
                ).build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)

        }
    }

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando registro nao existir`() {
        // ação
        val pixIdNaoExistente = UUID.randomUUID().toString()
        val clienteIdNaoExistente = UUID.randomUUID().toString()
        val thrown = org.junit.jupiter.api.assertThrows<StatusRuntimeException> {
            grpcClient.carrega(ConsultaChavePixRequest.newBuilder()
                .setPixId(FiltroPorPixId.newBuilder()
                    .setPixId(pixIdNaoExistente)
                    .setClienteId(clienteIdNaoExistente)
                    .build()
                ).build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro existir localmente`() {
        // cenário
        val chaveExistente = repository.findByChave("rafael.ponte@zup.com.br").get()

        // ação
        val response = grpcClient.carrega(ConsultaChavePixRequest.newBuilder()
            .setChave("rafael.ponte@zup.com.br")
            .build())

        // validação
        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipoDaChave.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro nao existir localmente mas existir no BCB`() {
        // cenário
        val bcbResponse = pixKeyDetailsResponse()
        Mockito.`when`(erpBcb.findByKey(key = "user.from.another.bank@santander.com.br"))
            .thenReturn(HttpResponse.ok(pixKeyDetailsResponse()))

        // ação
        val response = grpcClient.carrega(ConsultaChavePixRequest.newBuilder()
            .setChave("user.from.another.bank@santander.com.br")
            .build())

        // validação
        with(response) {
            assertEquals("", this.pixId)
            assertEquals("", this.clienteId)
            assertEquals(bcbResponse.keyType.name, this.chave.tipo.name)
            assertEquals(bcbResponse.key, this.chave.chave)
        }
    }

    @Test
    fun `nao deve carregar chave por valor da chave quando registro nao existir localmente nem no BCB`() {
        // cenário
        Mockito.`when`(erpBcb.findByKey(key = "not.existing.user@santander.com.br"))
            .thenReturn(HttpResponse.notFound())

        // ação
        val thrown = org.junit.jupiter.api.assertThrows<StatusRuntimeException> {
            grpcClient.carrega(ConsultaChavePixRequest.newBuilder()
                .setChave("not.existing.user@santander.com.br")
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `nao deve carregar chave por valor da chave quando filtro invalido`() {
        // ação
        val thrown = org.junit.jupiter.api.assertThrows<StatusRuntimeException> {
            grpcClient.carrega(ConsultaChavePixRequest.newBuilder().setChave("").build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)

        }
    }

    @Test
    fun `nao deve carregar chave quando filtro invalido`() {

        // ação
        val thrown = org.junit.jupiter.api.assertThrows<StatusRuntimeException> {
            grpcClient.carrega(ConsultaChavePixRequest.newBuilder().build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix inválida ou não informada", status.description)
        }
    }

    @MockBean(ErpBcb::class)
    fun erpBcb(): ErpBcb? {
        return Mockito.mock(ErpBcb::class.java)
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeymanagerConsultaGrpc.KeymanagerConsultaBlockingStub? {
            return KeymanagerConsultaGrpc.newBlockingStub(channel)
        }
    }

    private fun chave(
        tipo: TipoDaChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID(),
    ): ChavePix {
        return ChavePix(
            clienteId = clienteId,
            tipoDaChave = tipo,
            chave = chave,
            tipoDaConta = CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "UNIBANCO ITAU",
                nomeDoTitular = "Rafael Ponte",
                cpfDoTitular = "12345678900",
                agencia = "1218",
                numeroDaConta = "123456"
            )
        )
    }

    private fun pixKeyDetailsResponse(): PixKeyDetailsResponse {
        return PixKeyDetailsResponse(
            keyType = PixKeyType.EMAIL,
            key = "user.from.another.bank@santander.com.br",
            bankAccount = bankAccount(),
            owner = owner(),
            createdAt = LocalDateTime.now()
        )
    }

    private fun bankAccount(): BankAccount {
        return BankAccount(
            participant = "90400888",
            branch = "9871",
            accountNumber = "987654",
            accountType = BankAccount.AccountType.SVGS
        )
    }

    private fun owner(): Owner {
        return Owner(
            type = Owner.OwnerType.NATURAL_PERSON,
            name = "Another User",
            taxIdNumber = "12345678901"
        )
    }
}