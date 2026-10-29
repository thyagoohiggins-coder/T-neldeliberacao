package com.hikins.tunelfai.model

/** Status de um veículo no túnel. Sem status (null) = "Fazendo Inspeção 360" (não contabiliza). */
enum class Status(val texto: String) {
    APROVADO("🟢 Aprovado"),
    REPARO("Reparo")
}

/** VIN a partir do código de barras do chassi. */
object Vin {
    /** Usa tudo o que foi lido no código, só em maiúsculas e sem espaços/símbolos. */
    fun limpar(lido: String): String = lido.uppercase().filter { it.isLetterOrDigit() }
}

/** Modelos e cores disponíveis (opcionais, só para identificar o carro no app). */
object Catalogo {
    val modelos = listOf(
        "Uni-T", "CS55", "CS75", "Tiggo 7 Pro Adas", "Tiggo 7 Ice Low", "Tiggo 8 Ice", "Tiggo 5 Ice Low"
    )
    val cores = listOf("Cinza", "Branco", "Preto", "Azul")
}

/**
 * Um veículo registrado no Túnel de liberação.
 * @param ordem sequência global de adição, usada para ordenar dentro do intervalo.
 */
data class Registro(
    val vin: String,
    val modelo: String?,
    val cor: String?,
    val status: Status?,      // null = Fazendo Inspeção 360
    val reparo: String?,      // descrição do reparo (só quando REPARO)
    val intervalo: String,    // ex.: "13:00"
    val ordem: Long,
    val responsavel: String? = null,          // responsável pelo reparo (só quando REPARO)
    val fotos: List<String> = emptyList()     // caminhos das fotos do demérito (só quando REPARO)
) {
    /** Só entra na Produção quem já tem status (Aprovado ou Reparo). */
    val contabiliza: Boolean get() = status != null

    val textoStatus: String
        get() = when (status) {
            null -> "Fazendo Inspeção 360"
            Status.APROVADO -> Status.APROVADO.texto
            Status.REPARO -> "Reparo: ${reparo.orEmpty()}"
        }
}
