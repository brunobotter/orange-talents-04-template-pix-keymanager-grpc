package br.com.bruno.orange.pixdesafio.pix.registrar

import br.com.bruno.orange.pixdesafio.erros.exception.ChavePixExistenteException
import br.com.bruno.orange.pixdesafio.externo.bcb.CreatePixKeyRequest
import br.com.bruno.orange.pixdesafio.externo.bcb.ErpBcb
import br.com.bruno.orange.pixdesafio.externo.itau.ErpItau
import br.com.bruno.orange.pixdesafio.pix.ChavePix
import br.com.bruno.orange.pixdesafio.pix.ChavePixRepository
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated

import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(@Inject val repository: ChavePixRepository,
                          @Inject val erpItau: ErpItau,
                            @Inject val erpBcb: ErpBcb) {

    @Transactional
    fun salvar(@Valid novaChavePix: NovaChavePix?): ChavePix {
        //Verifica se a chave ja existe no sistema
        if(repository.existsByChave(novaChavePix?.chave)) {
            throw ChavePixExistenteException("Chave ja cadastrada")
        }
        //consulta no sistema do ERP do ITAU
        val erpItauResponse = erpItau.buscaContaPorTipo(novaChavePix?.clienteId!!, novaChavePix.tipoDaConta!!.name)
        val conta = erpItauResponse.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        //salva no banco de dados
        val novaChave = novaChavePix.toModel(conta)
        repository.save(novaChave)
        //envia para bcb gerar a chave pix
        val erpBcbResquest = CreatePixKeyRequest.of(novaChave)
        val erpResponse = erpBcb.create(erpBcbResquest)
        if(erpResponse.status != HttpStatus.CREATED)
            throw IllegalStateException("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)")
        //atualiza a chave de dominio com a chavepix do bcb
        novaChave.atualiza(erpResponse.body().key)
        return novaChave
    }
}
