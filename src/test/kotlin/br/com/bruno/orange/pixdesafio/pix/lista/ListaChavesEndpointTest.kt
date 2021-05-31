package br.com.bruno.orange.pixdesafio.pix.lista

import br.com.bruno.orange.pixdesafio.KeymanagerListaGrpc
import br.com.bruno.orange.pixdesafio.ListaChavePixRequest
import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaConta
import br.com.bruno.orange.pixdesafio.pix.ChavePix
import br.com.bruno.orange.pixdesafio.pix.ChavePixRepository
import br.com.bruno.orange.pixdesafio.pix.ContaAssociada
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.inject.Singleton
@MicronautTest(transactional = false)
internal class ListaChavesEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeymanagerListaGrpc.KeymanagerListaBlockingStub,
) {
    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    /**
     * TIP: por padrão roda numa transação isolada
     */
    @BeforeEach
    fun setup() {
        repository.save(chave(tipo = TipoDaChave.EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = TipoDaChave.ALEATORIA, chave = "randomkey-2", clienteId = UUID.randomUUID()))
        repository.save(chave(tipo = TipoDaChave.ALEATORIA, chave = "randomkey-3", clienteId = CLIENTE_ID))
    }

    /**
     * TIP: por padrão roda numa transação isolada
     */
    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve listar todas as chaves do cliente`() {
        // cenário
        val clienteId = CLIENTE_ID.toString()

        // ação
        val response = grpcClient.lista(ListaChavePixRequest.newBuilder()
            .setClienteId(clienteId)
            .build())

        // validação
        with(response.chavesList) {
            assertThat(this, hasSize(2))
            assertThat(
                this.map { Pair(it.tipoDaChave, it.chave) }.toList(),
                containsInAnyOrder(
                    Pair(TipoDaChave.ALEATORIA, "randomkey-3"),
                    Pair(TipoDaChave.EMAIL, "rafael.ponte@zup.com.br")
                )
            )
        }
    }


    @Test
    fun `nao deve listar as chaves do cliente quando cliente nao possuir chaves`() {
        // cenário
        val clienteSemChaves = UUID.randomUUID().toString()

        // ação
        val response = grpcClient.lista(ListaChavePixRequest.newBuilder()
            .setClienteId(clienteSemChaves)
            .build())

        // validação
        assertEquals(0, response.chavesCount)
    }

    @Test
    fun `nao deve listar todas as chaves do cliente quando clienteId for invalido`() {
        // cenário
        val clienteIdInvalido = ""

        // ação
        val thrown = org.junit.jupiter.api.assertThrows<StatusRuntimeException> {
            grpcClient.lista(ListaChavePixRequest.newBuilder()
                .setClienteId(clienteIdInvalido)
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Cliente ID não pode ser nulo ou vazio", status.description)
        }
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeymanagerListaGrpc.KeymanagerListaBlockingStub? {
            return KeymanagerListaGrpc.newBlockingStub(channel)
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