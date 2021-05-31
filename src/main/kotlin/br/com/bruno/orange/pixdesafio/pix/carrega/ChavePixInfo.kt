package br.com.bruno.orange.pixdesafio.pix.carrega

import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaConta
import br.com.bruno.orange.pixdesafio.pix.ChavePix
import br.com.bruno.orange.pixdesafio.pix.ContaAssociada
import java.time.LocalDateTime
import java.util.*

data class ChavePixInfo(
    val pixId: UUID? = null,
    val clienteId: UUID? = null,
    val tipo: TipoDaChave,
    val chave: String,
    val tipoDeConta: TipoDaConta,
    val conta: ContaAssociada,
    val registradaEm: LocalDateTime = LocalDateTime.now()
) {

    companion object {
        fun of(chave: ChavePix): ChavePixInfo {
            return ChavePixInfo(
                pixId = chave.id,
                clienteId = chave.clienteId,
                tipo = chave.tipoDaChave,
                chave = chave.chave,
                tipoDeConta = chave.tipoDaConta,
                conta = chave.conta,
                registradaEm = chave.criadaEm
            )
        }
    }
}