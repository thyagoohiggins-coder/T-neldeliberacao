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
/** Modelo: código interno, nome (vai na mensagem) e prefixo do chassi que identifica o modelo. */
data class Modelo(val codigo: String, val nome: String, val prefixo: String)

object Catalogo {
    val modelos = listOf(
        Modelo("S202", "Uni-T", "95PLDL61DVB"),
        Modelo("C655", "CS55", "95PJCL61DVB"),
        Modelo("C928", "CS75", "95PKDM81DVB"),
        Modelo("T1E ADAS", "Tiggo 7 Adas", "95PEEL61DVB"),
        Modelo("T1E ICE LOW", "Tiggo 7 Ice Low", "95PEFL31DVB"),
        Modelo("T19 ICE LOW", "Tiggo 5 Ice Low", "95PBFK31DVB"),
        Modelo("T19 ICE HIGH", "Tiggo 5 Ice High", "95PBEK31DVB"),
        Modelo("T18", "Tiggo 8 Ice", "95PFEM61DVB")
    )
    val cores = listOf("Cinza", "Branco", "Preto", "Azul")

    /** Modelo pelo prefixo do chassi (ex.: "95PLDL61DVB007163" -> Uni-T). Null se não reconhecer. */
    fun detectar(vin: String): Modelo? {
        val v = Vin.limpar(vin)
        return modelos.firstOrNull { v.startsWith(it.prefixo) }
    }

    /** Nomes antigos salvos no app -> nome atual. */
    fun nomeAtual(nome: String): String = when (nome) {
        "Tiggo 7", "Tiggo 7 Pro Adas" -> "Tiggo 7 Adas"
        else -> nome
    }
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
