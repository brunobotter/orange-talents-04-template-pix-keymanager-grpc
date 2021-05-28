package br.com.bruno.orange.pixdesafio.externo.itau

import br.com.bruno.orange.pixdesafio.pix.DadosDaContaResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue


@Client("\${itau.contas.url}")
interface ErpItau {
    @Get("/api/v1/clientes/{clienteId}/contas{?tipo}")
    fun buscaContaPorTipo(@PathVariable clienteId: String?, @QueryValue tipo: String?): HttpResponse<DadosDaContaResponse>

}