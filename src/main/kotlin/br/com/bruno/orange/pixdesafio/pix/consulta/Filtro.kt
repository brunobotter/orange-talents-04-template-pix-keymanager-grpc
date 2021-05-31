package br.com.bruno.orange.pixdesafio.pix.consulta

import br.com.bruno.orange.pixdesafio.erros.exception.ChavePixNaoExistenteException
import br.com.bruno.orange.pixdesafio.externo.bcb.ErpBcb
import br.com.bruno.orange.pixdesafio.pix.ChavePixRepository
import br.com.bruno.orange.pixdesafio.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
sealed class Filtro {

    abstract fun filtra(repository: ChavePixRepository, erpBcb: ErpBcb): ChavePixInfo

    @Introspected
    data class PorPixId(
        @field:NotBlank @field:ValidUUID val clienteId: String, // 1
        @field:NotBlank @field:ValidUUID val pixId: String,
    ) : Filtro() {
        fun pixIdAsUuid() = UUID.fromString(pixId)
        fun clienteIdAsUuid() = UUID.fromString(clienteId)

        override fun filtra(repository: ChavePixRepository, erpBcb: ErpBcb): ChavePixInfo {
            return repository.findById(pixIdAsUuid())
                .filter { it.pertenceAo(clienteIdAsUuid()) }
                .map(ChavePixInfo::of)
                .orElseThrow { ChavePixNaoExistenteException("Chave Pix não encontrada") }
        }
    }


    @Introspected
    data class PorChave(@field:NotBlank @Size(max = 77) val chave: String) : Filtro() { // 1

        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        override fun filtra(repository: ChavePixRepository, erpBcb: ErpBcb): ChavePixInfo {
            return repository.findByChave(chave)
                .map(ChavePixInfo::of)
                .orElseGet {
                    LOGGER.info("Consultando chave Pix '$chave' no Banco Central do Brasil (BCB)")

                    val response = erpBcb.findByKey(chave) // 1
                    when (response.status) { // 1
                        HttpStatus.OK -> response.body()?.toModel() // 1
                        else -> throw ChavePixNaoExistenteException("Chave Pix não encontrada") // 1
                    }
                }
        }
    }

    @Introspected
    class Invalido() : Filtro() {

        override fun filtra(repository: ChavePixRepository, erpBcb: ErpBcb): ChavePixInfo {
            throw IllegalArgumentException("Chave Pix inválida ou não informada")
        }
    }
}
