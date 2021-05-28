package br.com.bruno.orange.pixdesafio.pix

import br.com.bruno.orange.pixdesafio.TipoDaChave
import br.com.bruno.orange.pixdesafio.TipoDaConta
import java.util.*
import javax.persistence.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
class ChavePix(
    @field:NotNull
    val clienteId: UUID,
    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipoDaChave: TipoDaChave,
    @field:NotBlank val chave: String,
    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipoDaConta: TipoDaConta,
    @field:Valid
    @Embedded
    val conta: ContaAssociada
    )
{

    @Id
    @GeneratedValue
    val id: UUID? = null
    override fun toString(): String {
        return "ChavePix(clienteId=$clienteId, tipoDaChave=$tipoDaChave, chave='$chave', tipoDaConta=$tipoDaConta, id=$id)"
    }


}
