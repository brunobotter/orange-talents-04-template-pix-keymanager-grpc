package br.com.bruno.orange.pixdesafio.pix.registrar

import br.com.bruno.orange.pixdesafio.RegistraChavePixRequest
import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaChave.UNKNOWN_TIPO_CHAVE
import br.com.bruno.orange.pixdesafio.TipoDaConta
import br.com.bruno.orange.pixdesafio.TipoDaConta.UNKNOWN_TIPO_CONTA


fun RegistraChavePixRequest.toModel(): NovaChavePix{
    return NovaChavePix(
        clienteId = clienteId,
        tipoDaChave = when(tipoDaChave){
            UNKNOWN_TIPO_CHAVE -> null
            else -> TipoDaChave.valueOf(tipoDaChave.name)
        },
        tipoDaConta = when(tipoDaConta){
             UNKNOWN_TIPO_CONTA -> null
            else -> TipoDaConta.valueOf(tipoDaConta.name)
        },
        chave = valorDaChave
    )
}