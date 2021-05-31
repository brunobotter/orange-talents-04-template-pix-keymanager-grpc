package br.com.bruno.orange.pixdesafio.pix.deletar

import br.com.bruno.orange.pixdesafio.erros.exception.ChavePixNaoExistenteException
import br.com.bruno.orange.pixdesafio.externo.bcb.DeletePixKeyRequest
import br.com.bruno.orange.pixdesafio.externo.bcb.ErpBcb
import br.com.bruno.orange.pixdesafio.pix.ChavePixRepository
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Validated
@Singleton
class DeletaChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val erpBcb: ErpBcb
) {

    @Transactional
    fun deletar(@NotBlank @NotNull clienteId: String?,@NotBlank @NotNull pixId: String?) {

        val uuidCLienteId = UUID.fromString(clienteId)
        val uuidPixId = UUID.fromString(pixId)
        val chavePix = repository.findByIdAndClienteId(uuidPixId,uuidCLienteId)
            .orElseThrow{ ChavePixNaoExistenteException("Chave pix nao cadastrada ou nao pertence ao cliente")}
        val request = DeletePixKeyRequest(chavePix.chave)
        val response = erpBcb.delete(key = chavePix.chave, request)
        if(response.status != HttpStatus.OK){
            throw IllegalStateException("Erro ao remover chave pix do banco central do brasil (BCB)")
        }
        repository.delete(chavePix)
        }

    }



