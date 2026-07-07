package com.hikins.tunelfai.model

/**
 * Configurações do app, ajustadas pelo usuário na tela de Configurações.
 *
 * @param exigirFoto obriga anexar ao menos 1 foto por VIN para poder adicioná-lo
 * @param motivoAtivado se falso, o motivo do VIN negado nem aparece nem é enviado
 * @param motivoLivre se verdadeiro, o motivo é escrito à mão; senão usa a lista padrão
 * @param vibracaoAtivada liga o lembrete "Fazer Relatório" 10 min antes de cada intervalo
 * @param vibracaoSegundos duração da vibração forte do lembrete
 * @param destacarNegrito envolve os prefixos (VIN, Status, ...) com "*" p/ negrito no WhatsApp
 */
data class Configuracao(
    val exigirFoto: Boolean = false,
    val motivoAtivado: Boolean = true,
    val motivoLivre: Boolean = false,
    val vibracaoAtivada: Boolean = false,
    val vibracaoSegundos: Int = 3,
    val destacarNegrito: Boolean = true
)
