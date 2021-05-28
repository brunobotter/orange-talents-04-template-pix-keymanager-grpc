package br.com.bruno.orange.pixdesafio.pix.registrar

import br.com.bruno.orange.pixdesafio.erros.exception.ChavePixExistenteException
import br.com.bruno.orange.pixdesafio.externo.itau.ErpItau
import br.com.bruno.orange.pixdesafio.pix.ChavePix
import io.micronaut.validation.Validated

import java.lang.RuntimeException
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(@Inject val repository: ChavePixRepository,
                          @Inject val erpItau: ErpItau) {

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
        return novaChave
    }
}
