package br.com.bruno.orange.pixdesafio.pix.registrar

import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaConta
import br.com.bruno.orange.pixdesafio.pix.ChavePix
import br.com.bruno.orange.pixdesafio.pix.ContaAssociada
import br.com.bruno.orange.pixdesafio.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Introspected
data class NovaChavePix(
    @field:NotBlank @ValidUUID val clienteId: String,
    @field:NotNull val tipoDaChave: TipoDaChave?,
    @field:NotNull val tipoDaConta: TipoDaConta?,
    @field:Size(max = 77) val chave: String
) {


    fun toModel(conta: ContaAssociada): ChavePix{
        return ChavePix(
            clienteId = UUID.fromString(this.clienteId),
            tipoDaChave = TipoDaChave.valueOf(this.tipoDaChave!!.name),
            tipoDaConta = TipoDaConta.valueOf(this.tipoDaConta!!.name),
            //se o tipo da chave for aleatorio entao ira gerar uma chave aleatoria, senao ira pegar a chave que esta vindo
            chave = if(this.tipoDaChave == TipoDaChave.ALEATORIA) UUID.randomUUID().toString() else this.chave,
            conta = conta
        )
    }

}
