package com.hikins.tunelfai.model

/** Status de um veículo no túnel. Sem status (null) = "Fazendo Inspeção 360" (não contabiliza). */
enum class Status(val texto: String) {
    APROVADO("🟢 Aprovado"),
    REPARO("Reparo"),
    BLOQUEADO("Bloqueado"),   // não entra nas estatísticas
    SEPARADO("Separado")      // não entra nas estatísticas
}

/** VIN a partir do código de barras do chassi. */
object Vin {
    /** Usa tudo o que foi lido no código, só em maiúsculas e sem espaços/símbolos. */
    fun limpar(lido: String): String = lido.uppercase().filter { it.isLetterOrDigit() }
}

/** Modelos e cores disponíveis (opcionais, só para identificar o carro no app). */
enum class Marca { CHERY, CHANGAN }

/** Modelo: código interno, nome (vai na mensagem), prefixo do chassi e marca. */
data class Modelo(val codigo: String, val nome: String, val prefixo: String, val marca: Marca)

object Catalogo {
    val modelos = listOf(
        Modelo("S202", "Uni-T", "95PLDL61DVB", Marca.CHANGAN),
        Modelo("C655", "CS55", "95PJCL61DVB", Marca.CHANGAN),
        Modelo("C928", "CS75", "95PKDM81DVB", Marca.CHANGAN),
        Modelo("T1E ADAS", "Tiggo 7 Adas", "95PEEL61DVB", Marca.CHERY),
        Modelo("T1E ICE LOW", "Tiggo 7 Ice Low", "95PEFL31DVB", Marca.CHERY),
        Modelo("T19 ICE LOW", "Tiggo 5 Ice Low", "95PBFK31DVB", Marca.CHERY),
        Modelo("T19 ICE HIGH", "Tiggo 5 Ice High", "95PBEK31DVB", Marca.CHERY),
        Modelo("T18", "Tiggo 8 Ice", "95PFEM61DVB", Marca.CHERY)
    )
    val cores = listOf("Cinza", "Branco", "Preto", "Azul")

    /** Caracteres do prefixo que identificam o modelo (fabricante + descrição do veículo). */
    private const val TAMANHO_CHAVE = 8

    /**
     * Modelo pelo prefixo do chassi (ex.: "95PLDL61DVB007163" -> Uni-T). Compara só os 8 primeiros
     * caracteres: os seguintes (dígito, ano, fábrica) mudam de um carro/ano para outro.
     */
    fun detectar(vin: String): Modelo? {
        val v = Vin.limpar(vin)
        if (v.length < TAMANHO_CHAVE) return null
        return modelos.firstOrNull { v.startsWith(it.prefixo.take(TAMANHO_CHAVE)) }
    }

    /** Marca pelo nome do modelo escolhido ou, se não houver, pelo prefixo do VIN. */
    fun marcaDe(modelo: String?, vin: String): Marca? =
        modelos.firstOrNull { it.nome == modelo }?.marca ?: detectar(vin)?.marca

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
    val responsavel: String? = null,          // responsável (Reparo, Bloqueado ou Separado)
    val fotos: List<String> = emptyList(),    // fotos do demérito (Reparo, Bloqueado ou Separado)
    val motivo: String? = null,               // motivo (só Bloqueado/Separado)
    val observacao: String? = null            // observação (só Bloqueado/Separado)
) {
    /** Só entra na Produção quem é Aprovado ou Reparo (Inspeção, Bloqueado e Separado não contam). */
    val contabiliza: Boolean get() = status == Status.APROVADO || status == Status.REPARO

    val textoStatus: String
        get() = when (status) {
            null -> "Fazendo Inspeção 360"
            Status.APROVADO -> Status.APROVADO.texto
            Status.REPARO -> "Reparo: ${reparo.orEmpty()}"
            Status.BLOQUEADO -> "Bloqueado"
            Status.SEPARADO -> "Separado"
        }
}
