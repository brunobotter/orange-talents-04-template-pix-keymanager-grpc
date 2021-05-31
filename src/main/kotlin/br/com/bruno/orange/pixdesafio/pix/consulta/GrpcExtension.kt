package br.com.bruno.orange.pixdesafio.pix.consulta

import br.com.bruno.orange.pixdesafio.ConsultaChavePixRequest
import javax.validation.ConstraintViolationException

import br.com.bruno.orange.pixdesafio.ConsultaChavePixRequest.FiltroCase.*
import javax.validation.Validator

fun ConsultaChavePixRequest.toModel(validator: Validator): Filtro{
    val filtro = when(filtroCase!!) { // 1
        PIXID -> pixId.let { // 1
            Filtro.PorPixId(clienteId = it.clienteId, pixId = it.pixId) // 1
        }
        CHAVE -> Filtro.PorChave(chave) // 2
        FILTRO_NOT_SET -> Filtro.Invalido() // 2
    }

    val violations = validator.validate(filtro)
    if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations);
    }

    return filtro
}