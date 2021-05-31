package br.com.bruno.orange.pixdesafio.erros.handler

import br.com.bruno.orange.pixdesafio.erros.exception.ChavePixExistenteException
import br.com.bruno.orange.pixdesafio.erros.exception.ChavePixNaoExistenteException
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class ChavePixNaoExisteExceptionHandler: ExceptionHandler<ChavePixNaoExistenteException> {

    override fun handle(e: ChavePixNaoExistenteException): ExceptionHandler.StatusWithDetails {
        return ExceptionHandler.StatusWithDetails(Status.NOT_FOUND
            .withDescription(e.message)
            .withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return e is ChavePixNaoExistenteException
    }
}