package br.com.bruno.orange.pixdesafio.pix.consulta

import br.com.bruno.orange.pixdesafio.ConsultaChavePixResponse
import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaConta
import com.google.protobuf.Timestamp
import java.time.ZoneId

class ConsultaChavePixResponseConverter {
    fun convert(chaveInfo: ChavePixInfo): ConsultaChavePixResponse {
        return ConsultaChavePixResponse.newBuilder()
            .setClienteId(chaveInfo.clienteId?.toString() ?: "") // Protobuf usa "" como default value para String
            .setPixId(chaveInfo.pixId?.toString() ?: "") // Protobuf usa "" como default value para String
            .setChave(ConsultaChavePixResponse.ChavePix // 1
                .newBuilder()
                .setTipo(TipoDaChave.valueOf(chaveInfo.tipo.name)) // 2
                .setChave(chaveInfo.chave)
                .setConta(ConsultaChavePixResponse.ChavePix.ContaInfo.newBuilder() // 1
                    .setTipo(TipoDaConta.valueOf(chaveInfo.tipoDeConta.name)) // 2
                    .setInstituicao(chaveInfo.conta.instituicao) // 1 (Conta)
                    .setNomeDoTitular(chaveInfo.conta.nomeDoTitular)
                    .setCpfDoTitular(chaveInfo.conta.cpfDoTitular)
                    .setAgencia(chaveInfo.conta.agencia)
                    .setNumeroDaConta(chaveInfo.conta.numeroDaConta)
                    .build()
                )
                .setCriadaEm(chaveInfo.registradaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
            )
            .build()
    }
}
