package com.hikins.tunelfai.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hikins.tunelfai.data.Configuracoes
import com.hikins.tunelfai.data.EstadoTunel
import com.hikins.tunelfai.data.Repositorio
import com.hikins.tunelfai.lembrete.Lembretes
import com.hikins.tunelfai.model.Catalogo
import com.hikins.tunelfai.model.Intervalos
import com.hikins.tunelfai.model.Registro
import com.hikins.tunelfai.model.Status
import com.hikins.tunelfai.update.Atualizador
import com.hikins.tunelfai.update.InfoAtualizacao
import kotlinx.coroutines.launch
import java.util.Calendar

private val CorMeta = Color(0xFF64B5F6)
private val CorVerde = Color(0xFF66BB6A)
private val CorVermelho = Color(0xFFEF5350)
private val CorReparo = Color(0xFFFFB74D)
private val CorInspecao = Color(0xFFB0BEC5)

private const val NENHUM = "—"
private const val AUTOMATICO = "Automático"
private const val TXT_INSPECAO = "Em branco (Fazendo Inspeção 360)"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunelApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { Repositorio(context) }
    val config = remember { Configuracoes(context) }

    val registros = remember { mutableStateListOf<Registro>() }
    var seq by remember { mutableStateOf(0L) }

    var menuAberto by remember { mutableStateOf(false) }
    var confirmarLimpar by remember { mutableStateOf(false) }
    var mostrarConfig by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Registro?>(null) }
    // Seleção múltipla (pela "ordem" de cada registro) para mover/excluir em grupo.
    val selecionados = remember { mutableStateListOf<Long>() }
    var escolherDestino by remember { mutableStateOf(false) }
    var confirmarExcluirSel by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<InfoAtualizacao?>(null) }
    var mostrarDialogoUpdate by remember { mutableStateOf(false) }

    var adiantar by remember { mutableStateOf(config.adiantarIntervalo) }
    var lembretes by remember { mutableStateOf(config.lembretes) }

    val pedirNotificacao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        if (!ok) {
            lembretes = false; config.lembretes = false
            Toast.makeText(context, "Sem permissão de notificação, os lembretes ficam desligados.",
                Toast.LENGTH_LONG).show()
        }
        Lembretes.atualizar(context)
    }

    fun persistir() {
        repo.salvar(EstadoTunel(registros.toList(), seq))
    }

    LaunchedEffect(Unit) {
        val e = repo.carregar()
        registros.addAll(e.registros)
        seq = e.seq
        Lembretes.atualizar(context)
        // Checagem silenciosa de atualização ao abrir.
        val r = Atualizador.verificar(context)
        if (r != null && r.temNova) {
            info = r
            mostrarDialogoUpdate = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hora a Hora Túnel FAI", fontWeight = FontWeight.Bold)
                        Text("v${Atualizador.versaoAtual(context)}",
                            style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
                actions = {
                    IconButton(onClick = { menuAberto = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                        DropdownMenuItem(
                            text = { Text("Configurações") },
                            onClick = { menuAberto = false; mostrarConfig = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Verificar atualização") },
                            onClick = {
                                menuAberto = false
                                scope.launch {
                                    val r = Atualizador.verificar(context)
                                    when {
                                        r == null -> Toast.makeText(context,
                                            "Sem conexão ou nenhuma release encontrada.",
                                            Toast.LENGTH_LONG).show()
                                        r.temNova -> { info = r; mostrarDialogoUpdate = true }
                                        else -> Toast.makeText(context,
                                            "Você já está na versão mais recente (${r.versaoAtual}).",
                                            Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Limpar tudo") },
                            onClick = { menuAberto = false; confirmarLimpar = true }
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (selecionados.isNotEmpty()) {
                Surface(tonalElevation = 6.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selecionados.clear() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancelar seleção")
                        }
                        Text("${selecionados.size} selecionado(s)", modifier = Modifier.weight(1f))
                        TextButton(onClick = { escolherDestino = true }) { Text("Mover") }
                        TextButton(onClick = { confirmarExcluirSel = true }) {
                            Text("Excluir", color = CorVermelho)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.width(0.dp)) }

            // Formulário de registro
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    FormVeiculo(
                        inicial = null,
                        adiantar = adiantar,
                        onSalvar = { r ->
                            seq += 1
                            registros.add(r.copy(ordem = seq))
                            persistir()
                            Toast.makeText(context, "Veículo adicionado no intervalo ${r.intervalo}.",
                                Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Intervalos presentes, em ordem decrescente (15:00 no topo)
            val intervalosPresentes = registros.map { it.intervalo }.distinct().sortedDescending()
            items(intervalosPresentes, key = { it }) { intervalo ->
                GrupoIntervalo(
                    intervalo = intervalo,
                    registros = registros,
                    selecionados = selecionados,
                    onEditar = { editando = it },
                    onExcluir = { registros.remove(it); selecionados.remove(it.ordem); persistir() }
                )
            }

            // Resumo total
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("TOTAL", fontWeight = FontWeight.Bold, color = CorMeta)
                        PainelEstatistica(Formatador.estatisticaTotal(registros))
                        Button(
                            onClick = { enviarWhatsApp(context, Formatador.total(registros)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Formatar total e enviar")
                        }
                    }
                }
            }
            item { Spacer(Modifier.width(0.dp)) }
        }
    }

    editando?.let { atual ->
        Dialog(onDismissRequest = { editando = null }) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FormVeiculo(
                        inicial = atual,
                        adiantar = adiantar,
                        onSalvar = { novo ->
                            val i = registros.indexOfFirst { it.ordem == atual.ordem }
                            if (i >= 0) registros[i] = novo.copy(ordem = atual.ordem)
                            persistir()
                            editando = null
                        },
                        onCancelar = { editando = null }
                    )
                }
            }
        }
    }

    if (escolherDestino) {
        AlertDialog(
            onDismissRequest = { escolherDestino = false },
            title = { Text("Mover ${selecionados.size} veículo(s) para") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Intervalos.lista.forEach { destino ->
                        TextButton(
                            onClick = {
                                for (i in registros.indices) {
                                    if (registros[i].ordem in selecionados) {
                                        registros[i] = registros[i].copy(intervalo = destino)
                                    }
                                }
                                persistir()
                                Toast.makeText(context, "${selecionados.size} veículo(s) movido(s) para $destino.",
                                    Toast.LENGTH_SHORT).show()
                                selecionados.clear()
                                escolherDestino = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(Formatador.faixa(destino)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { escolherDestino = false }) { Text("Cancelar") }
            }
        )
    }

    if (confirmarExcluirSel) {
        AlertDialog(
            onDismissRequest = { confirmarExcluirSel = false },
            title = { Text("Excluir ${selecionados.size} veículo(s)?") },
            text = { Text("Os VINs selecionados serão removidos. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    registros.removeAll { it.ordem in selecionados }
                    selecionados.clear()
                    persistir()
                    confirmarExcluirSel = false
                }) { Text("Excluir", color = CorVermelho) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExcluirSel = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarConfig) {
        AlertDialog(
            onDismissRequest = { mostrarConfig = false },
            title = { Text("Configurações") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LinhaSwitch(
                        titulo = "Adiantar intervalo",
                        descricao = "Faltando ${Intervalos.MINUTOS_ADIANTAR} min ou menos para o próximo " +
                            "intervalo, o carro já vai para o intervalo seguinte.",
                        marcado = adiantar,
                        onMudar = { adiantar = it; config.adiantarIntervalo = it }
                    )
                    LinhaSwitch(
                        titulo = "Lembrete de fechamento",
                        descricao = "Notifica 15 min antes de cada intervalo (06:45 até 14:45) " +
                            "e às 15:35 no intervalo das 15:00.",
                        marcado = lembretes,
                        onMudar = {
                            lembretes = it; config.lembretes = it
                            if (it && Build.VERSION.SDK_INT >= 33) {
                                pedirNotificacao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Lembretes.atualizar(context)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarConfig = false }) { Text("Fechar") }
            }
        )
    }

    if (confirmarLimpar) {
        AlertDialog(
            onDismissRequest = { confirmarLimpar = false },
            title = { Text("Limpar tudo?") },
            text = { Text("Isso remove todos os veículos e estatísticas do dia. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    registros.clear(); selecionados.clear(); seq = 0
                    persistir()
                    confirmarLimpar = false
                }) { Text("Limpar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarLimpar = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDialogoUpdate && info != null) {
        val i = info!!
        AlertDialog(
            onDismissRequest = { mostrarDialogoUpdate = false },
            title = { Text("Atualização disponível") },
            text = { Text("Nova versão ${i.versaoNova} (atual ${i.versaoAtual}). Deseja baixar e instalar agora?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoUpdate = false
                    val url = i.urlApk
                    if (url != null) {
                        Toast.makeText(context, "Baixando atualização...", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            val ok = Atualizador.baixarEInstalar(context, url)
                            if (!ok) Toast.makeText(context,
                                "Falha ao baixar a atualização.", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Atualizar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoUpdate = false }) { Text("Agora não") }
            }
        )
    }
}

@Composable
private fun LinhaSwitch(titulo: String, descricao: String, marcado: Boolean, onMudar: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold)
            Text(descricao, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = marcado, onCheckedChange = onMudar)
    }
}

/**
 * Formulário de veículo. [inicial] null = novo registro (intervalo pode ser Automático);
 * senão edita o registro existente. [onSalvar] recebe o registro (ordem é ajustada por quem chama).
 */
@Composable
private fun FormVeiculo(
    inicial: Registro?,
    adiantar: Boolean,
    onSalvar: (Registro) -> Unit,
    onCancelar: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var vin by remember { mutableStateOf(inicial?.vin ?: "") }
    var modelo by remember { mutableStateOf(inicial?.modelo) }
    var cor by remember { mutableStateOf(inicial?.cor) }
    var status by remember { mutableStateOf(inicial?.status) }
    var reparo by remember { mutableStateOf(inicial?.reparo ?: "") }
    var intervalo by remember { mutableStateOf(inicial?.intervalo ?: AUTOMATICO) }

    fun intervaloAgora(): String {
        val c = Calendar.getInstance()
        return Intervalos.atual(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), adiantar)
    }

    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (inicial == null) "Registrar veículo" else "Editar veículo", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = vin,
            onValueChange = { vin = it.uppercase() },
            label = { Text("VIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth()
        )

        DropdownSelecao(
            rotulo = "Modelo (opcional)",
            valor = modelo ?: NENHUM,
            opcoes = listOf(NENHUM) + Catalogo.modelos,
            onSelecionar = { modelo = it.takeIf { m -> m != NENHUM } }
        )

        DropdownSelecao(
            rotulo = "Cor (opcional)",
            valor = cor ?: NENHUM,
            opcoes = listOf(NENHUM) + Catalogo.cores,
            onSelecionar = { cor = it.takeIf { c -> c != NENHUM } }
        )

        DropdownSelecao(
            rotulo = "Status",
            valor = status?.texto ?: TXT_INSPECAO,
            opcoes = listOf(TXT_INSPECAO) + Status.entries.map { it.texto },
            onSelecionar = { txt -> status = Status.entries.firstOrNull { it.texto == txt } }
        )

        if (status == Status.REPARO) {
            OutlinedTextField(
                value = reparo,
                onValueChange = { reparo = it },
                label = { Text("Reparo") },
                supportingText = { Text("Sem reparo escrito, fica como Fazendo Inspeção 360.") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        DropdownSelecao(
            rotulo = "Intervalo",
            valor = if (intervalo == AUTOMATICO) "$AUTOMATICO (agora: ${intervaloAgora()})" else intervalo,
            opcoes = (if (inicial == null) listOf(AUTOMATICO) else emptyList()) + Intervalos.lista,
            onSelecionar = { intervalo = it }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onCancelar != null) {
                OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            }
            Button(
                onClick = {
                    val v = vin.trim()
                    if (v.isBlank()) {
                        Toast.makeText(context, "Digite o VIN.", Toast.LENGTH_SHORT).show()
                    } else {
                    val rep = reparo.trim()
                    // Reparo sem descrição = Fazendo Inspeção 360 (não contabiliza).
                    val st = if (status == Status.REPARO && rep.isEmpty()) null else status
                    onSalvar(
                        Registro(
                            vin = v,
                            modelo = modelo,
                            cor = cor,
                            status = st,
                            reparo = if (st == Status.REPARO) rep else null,
                            intervalo = if (intervalo == AUTOMATICO) intervaloAgora() else intervalo,
                            ordem = inicial?.ordem ?: 0
                        )
                    )
                    if (inicial == null) {
                        vin = ""; modelo = null; cor = null; status = null; reparo = ""
                    }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (inicial == null) "Adicionar" else "Salvar") }
        }
    }
}

/** Meta (azul), Produção, Aprovado (verde) e Diferença (verde se bateu/passou, vermelho se negativa). */
@Composable
private fun PainelEstatistica(e: Estatistica) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ItemEstatistica("Meta", e.meta.toString(), CorMeta)
        ItemEstatistica("Produção", e.prod?.toString() ?: "-", MaterialTheme.colorScheme.onSurface)
        ItemEstatistica("Aprovado", e.aprov?.toString() ?: "-", CorVerde)
        val dif = e.dif
        ItemEstatistica(
            "Diferença",
            dif?.toString() ?: "-",
            when {
                dif == null -> MaterialTheme.colorScheme.onSurface
                dif < 0 -> CorVermelho
                else -> CorVerde
            }
        )
    }
}

@Composable
private fun ItemEstatistica(rotulo: String, valor: String, cor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(rotulo, style = MaterialTheme.typography.labelSmall)
        Text(valor, color = cor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun GrupoIntervalo(
    intervalo: String,
    registros: List<Registro>,
    selecionados: MutableList<Long>,
    onEditar: (Registro) -> Unit,
    onExcluir: (Registro) -> Unit
) {
    val context = LocalContext.current
    val doIntervalo = registros.filter { it.intervalo == intervalo }.sortedBy { it.ordem }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Formatador.faixa(intervalo), fontWeight = FontWeight.Bold)
                Text("${doIntervalo.size} veículo(s)")
            }
            val idsGrupo = doIntervalo.map { it.ordem }
            val todosMarcados = idsGrupo.isNotEmpty() && selecionados.containsAll(idsGrupo)
            TextButton(onClick = {
                if (todosMarcados) selecionados.removeAll(idsGrupo)
                else selecionados.addAll(idsGrupo.filterNot { it in selecionados })
            }) { Text(if (todosMarcados) "Desmarcar todos" else "Selecionar todos") }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            doIntervalo.forEachIndexed { i, r ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = r.ordem in selecionados,
                        onCheckedChange = { marcado ->
                            if (marcado) selecionados.add(r.ordem) else selecionados.remove(r.ordem)
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${i + 1}. ${r.vin}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val detalhes = listOfNotNull(r.modelo, r.cor).joinToString(" • ")
                        if (detalhes.isNotEmpty()) {
                            Text(detalhes, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            r.textoStatus,
                            color = when (r.status) {
                                null -> CorInspecao
                                Status.APROVADO -> CorVerde
                                Status.REPARO -> CorReparo
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { onEditar(r) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { onExcluir(r) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            PainelEstatistica(Formatador.estatistica(intervalo, registros))

            Button(
                onClick = { enviarWhatsApp(context, Formatador.intervalo(intervalo, registros)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Formatar intervalo e enviar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelecao(
    rotulo: String,
    valor: String,
    opcoes: List<String>,
    onSelecionar: (String) -> Unit
) {
    var aberto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = aberto,
        onExpandedChange = { aberto = it }
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(opcao) },
                    onClick = { onSelecionar(opcao); aberto = false }
                )
            }
        }
    }
}
