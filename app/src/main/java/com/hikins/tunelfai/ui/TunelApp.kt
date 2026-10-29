@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)

package com.hikins.tunelfai.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.layout.ContentScale
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.hikins.tunelfai.model.Vin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private const val AUTOMATICO = "Auto"
private const val PREFIXO_VIN = "VB"
private const val MAX_FOTOS = 4
private val RESPONSAVEIS = listOf("Trim Shop", "Small", "Fai", "Paint", "CKD", "Logística")

/** "06:00" -> "06:00 – 07:00" (versão de tela; a mensagem usa Formatador.faixa). */
private fun faixaCurta(intervalo: String): String =
    Formatador.faixa(intervalo).replace(" ÀS ", " – ")

/** Intervalo em andamento agora (null fora do turno 06:00–15:48). */
private fun intervaloEmAndamento(c: Calendar): String? {
    val h = c.get(Calendar.HOUR_OF_DAY)
    val m = c.get(Calendar.MINUTE)
    if (h !in 6..15 || (h == 15 && m >= 48)) return null
    return Intervalos.atual(h, m, false)
}

private fun corStatus(status: Status?): Color = when (status) {
    null -> Cores.Cinza
    Status.APROVADO -> Cores.Verde
    Status.REPARO -> Cores.Ambar
}

private fun rotuloStatus(status: Status?): String = when (status) {
    null -> "Inspeção 360"
    Status.APROVADO -> "Aprovado"
    Status.REPARO -> "Reparo"
}

@Composable
fun TunelApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { Repositorio(context) }
    val config = remember { Configuracoes(context) }
    val listState = rememberLazyListState()

    val registros = remember { mutableStateListOf<Registro>() }
    var seq by remember { mutableStateOf(0L) }

    var menuAberto by remember { mutableStateOf(false) }
    var confirmarLimpar by remember { mutableStateOf(false) }
    var mostrarConfig by remember { mutableStateOf(false) }
    var novoAberto by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Registro?>(null) }
    // Seleção múltipla (pela "ordem" de cada registro) para mover/excluir em grupo.
    val selecionados = remember { mutableStateListOf<Long>() }
    var escolherDestino by remember { mutableStateOf(false) }
    var confirmarExcluirSel by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<InfoAtualizacao?>(null) }
    var mostrarDialogoUpdate by remember { mutableStateOf(false) }

    var adiantar by remember { mutableStateOf(config.adiantarIntervalo) }
    var lembretes by remember { mutableStateOf(config.lembretes) }
    val bancoReparos = remember { mutableStateListOf<String>().apply { addAll(config.reparos) } }
    var novoReparo by remember { mutableStateOf("") }
    var agora by remember { mutableStateOf(Calendar.getInstance()) }
    var buscando by remember { mutableStateOf(false) }
    var busca by remember { mutableStateOf("") }
    var destacarRepetidos by remember { mutableStateOf(false) }

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

    // Atualiza o relógio da tela (intervalo em andamento).
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            agora = Calendar.getInstance()
        }
    }

    val emAndamento = intervaloEmAndamento(agora)
    val modoSelecao = selecionados.isNotEmpty()
    val filtro = busca.trim().uppercase()
    // Quantas vezes cada VIN aparece (para destacar os repetidos).
    val contagemVin = registros.groupingBy { it.vin.uppercase() }.eachCount()
    val qtdRepetidos = contagemVin.count { it.value > 1 }
    val intervalosPresentes = registros
        .filter { filtro.isEmpty() || it.vin.uppercase().contains(filtro) }
        .map { it.intervalo }.distinct().sortedDescending()

    fun alternar(id: Long) {
        if (id in selecionados) selecionados.remove(id) else selecionados.add(id)
    }

    Scaffold(
        containerColor = Cores.Fundo,
        topBar = {
            if (modoSelecao) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selecionados.clear() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancelar seleção")
                        }
                    },
                    title = { Text("${selecionados.size} selecionado(s)", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        TextButton(onClick = { escolherDestino = true }) { Text("Mover") }
                        IconButton(onClick = { confirmarExcluirSel = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = Cores.Vermelho)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Cores.AzulEscuro)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(Cores.Azul, Color(0xFF2357C5)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Túnel FAI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Hora a Hora • v${Atualizador.versaoAtual(context)}",
                                    style = MaterialTheme.typography.labelSmall, color = Cores.TextoSuave)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            buscando = !buscando
                            if (!buscando) busca = ""
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar VIN",
                                tint = if (buscando) Cores.Azul else LocalContentColor.current)
                        }
                        IconButton(onClick = { mostrarConfig = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                        }
                        IconButton(onClick = { menuAberto = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
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
                                text = { Text("Limpar tudo", color = Cores.Vermelho) },
                                onClick = { menuAberto = false; confirmarLimpar = true }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Cores.Fundo)
                )
            }
        },
        floatingActionButton = {
            if (!modoSelecao) {
                ExtendedFloatingActionButton(
                    onClick = { novoAberto = true },
                    containerColor = Cores.Azul,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Veículo", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "resumo") {
                CardResumo(
                    total = Formatador.estatisticaTotal(registros),
                    emAndamento = emAndamento,
                    onEnviar = { enviarWhatsApp(context, Formatador.total(registros)) }
                )
            }

            item(key = "ferramentas") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (buscando) {
                        OutlinedTextField(
                            value = busca,
                            onValueChange = { busca = it.uppercase() },
                            placeholder = { Text("Buscar VIN") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (busca.isNotEmpty()) {
                                    IconButton(onClick = { busca = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Limpar busca")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cores.Azul,
                                unfocusedBorderColor = Cores.Borda,
                                focusedContainerColor = Cores.Superficie,
                                unfocusedContainerColor = Cores.Superficie
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    FilterChip(
                        selected = destacarRepetidos,
                        onClick = { destacarRepetidos = !destacarRepetidos },
                        label = {
                            Text(
                                if (qtdRepetidos == 0) "Destacar VINs repetidos (nenhum)"
                                else "Destacar VINs repetidos ($qtdRepetidos)",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (qtdRepetidos > 0) Cores.Vermelho else Cores.TextoSuave)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Cores.Superficie,
                            labelColor = Cores.Texto,
                            selectedContainerColor = Cores.Vermelho.copy(alpha = 0.18f),
                            selectedLabelColor = Cores.Vermelho
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = destacarRepetidos,
                            borderColor = Cores.Borda,
                            selectedBorderColor = Cores.Vermelho
                        )
                    )
                }
            }

            if (registros.isEmpty()) {
                item(key = "vazio") { EstadoVazio() }
            } else if (intervalosPresentes.isEmpty()) {
                item(key = "sem-resultado") {
                    Text("Nenhum VIN encontrado para \"$filtro\".", color = Cores.TextoSuave,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp))
                }
            }

            items(intervalosPresentes, key = { it }) { intervalo ->
                CardIntervalo(
                    intervalo = intervalo,
                    registros = registros,
                    emAndamento = intervalo == emAndamento,
                    selecionados = selecionados,
                    filtro = filtro,
                    contagemVin = if (destacarRepetidos) contagemVin else emptyMap(),
                    onToque = { r -> if (modoSelecao) alternar(r.ordem) else editando = r },
                    onToqueLongo = { r -> alternar(r.ordem) },
                    onSelecionarGrupo = { ids ->
                        if (selecionados.containsAll(ids)) selecionados.removeAll(ids)
                        else selecionados.addAll(ids.filterNot { it in selecionados })
                    },
                    onEnviar = { enviarWhatsApp(context, Formatador.intervalo(intervalo, registros)) }
                )
            }
        }
    }

    if (novoAberto) {
        FolhaVeiculo(
            inicial = null,
            adiantar = adiantar,
            bancoReparos = bancoReparos,
            onFechar = { novoAberto = false },
            onSalvar = { r ->
                seq += 1
                registros.add(r.copy(ordem = seq))
                persistir()
                novoAberto = false
                Toast.makeText(context, "${r.vin} adicionado em ${faixaCurta(r.intervalo)}.",
                    Toast.LENGTH_SHORT).show()
            }
        )
    }

    editando?.let { atual ->
        FolhaVeiculo(
            inicial = atual,
            adiantar = adiantar,
            bancoReparos = bancoReparos,
            onFechar = { editando = null },
            onSalvar = { novo ->
                val i = registros.indexOfFirst { it.ordem == atual.ordem }
                if (i >= 0) registros[i] = novo.copy(ordem = atual.ordem)
                persistir()
                editando = null
            },
            onExcluir = {
                Fotos.apagar(atual.fotos)
                registros.removeAll { it.ordem == atual.ordem }
                selecionados.remove(atual.ordem)
                persistir()
                editando = null
            }
        )
    }

    if (escolherDestino) {
        AlertDialog(
            onDismissRequest = { escolherDestino = false },
            containerColor = Cores.Superficie,
            title = { Text("Mover ${selecionados.size} veículo(s)") },
            text = {
                Column {
                    Text("Escolha o intervalo de destino", color = Cores.TextoSuave,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Intervalos.lista.forEach { destino ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val n = selecionados.size
                                    for (i in registros.indices) {
                                        if (registros[i].ordem in selecionados) {
                                            registros[i] = registros[i].copy(intervalo = destino)
                                        }
                                    }
                                    persistir()
                                    selecionados.clear()
                                    escolherDestino = false
                                    Toast.makeText(context, "$n veículo(s) movido(s) para ${faixaCurta(destino)}.",
                                        Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(faixaCurta(destino)) }
                            )
                        }
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
            containerColor = Cores.Superficie,
            icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Cores.Vermelho) },
            title = { Text("Excluir ${selecionados.size} veículo(s)?") },
            text = { Text("Os VINs selecionados serão removidos. Não dá para desfazer.") },
            confirmButton = {
                Button(
                    onClick = {
                        Fotos.apagar(registros.filter { it.ordem in selecionados }.flatMap { it.fotos })
                        registros.removeAll { it.ordem in selecionados }
                        selecionados.clear()
                        persistir()
                        confirmarExcluirSel = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cores.Vermelho)
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExcluirSel = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarConfig) {
        ModalBottomSheet(
            onDismissRequest = { mostrarConfig = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Cores.Superficie
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Configurações", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LinhaSwitch(
                    titulo = "Adiantar intervalo",
                    descricao = "Faltando ${Intervalos.MINUTOS_ADIANTAR} min ou menos para o próximo " +
                        "intervalo, o carro já entra no intervalo seguinte.",
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

                // Banco de reparos
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Cores.SuperficieAlta)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Banco de reparos", fontWeight = FontWeight.SemiBold)
                    Text("Reparos pré-definidos que aparecem como sugestão ao escrever o reparo de um veículo.",
                        style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave)
                    fun adicionarReparo() {
                        val t = novoReparo.trim()
                        if (t.isNotEmpty() && bancoReparos.none { it.equals(t, ignoreCase = true) }) {
                            bancoReparos.add(t)
                            config.reparos = bancoReparos.toList()
                        }
                        novoReparo = ""
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = novoReparo,
                            onValueChange = { novoReparo = it },
                            placeholder = { Text("Novo reparo") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cores.Azul,
                                unfocusedBorderColor = Cores.Borda
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { adicionarReparo() },
                            enabled = novoReparo.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cores.Azul),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Adicionar reparo")
                        }
                    }
                    if (bancoReparos.isEmpty()) {
                        Text("Nenhum reparo cadastrado.", color = Cores.TextoSuave,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    bancoReparos.toList().forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Cores.Superficie)
                                .padding(start = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Cores.Ambar)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(item, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                bancoReparos.remove(item)
                                config.reparos = bancoReparos.toList()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remover reparo",
                                    tint = Cores.TextoSuave)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmarLimpar) {
        AlertDialog(
            onDismissRequest = { confirmarLimpar = false },
            containerColor = Cores.Superficie,
            title = { Text("Limpar tudo?") },
            text = { Text("Isso remove todos os veículos e estatísticas do dia. Não dá para desfazer.") },
            confirmButton = {
                Button(
                    onClick = {
                        Fotos.apagar(registros.flatMap { it.fotos })
                        registros.clear(); selecionados.clear(); seq = 0
                        persistir()
                        confirmarLimpar = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cores.Vermelho)
                ) { Text("Limpar") }
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
            containerColor = Cores.Superficie,
            title = { Text("Atualização disponível") },
            text = { Text("Nova versão ${i.versaoNova} (atual ${i.versaoAtual}). Deseja baixar e instalar agora?") },
            confirmButton = {
                Button(onClick = {
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

/* ---------------------------------------------------------------- Resumo */

@Composable
private fun CardResumo(total: Estatistica, emAndamento: String?, onEnviar: () -> Unit) {
    val prod = total.prod ?: 0
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1C3263), Color(0xFF141A26))))
            .border(1.dp, Color(0xFF2B3F6B), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FECHAMENTO DO DIA", style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFAFC4EE), letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
                if (emAndamento != null) {
                    Pilula("Agora ${faixaCurta(emAndamento)}", Cores.Azul)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$prod", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    lineHeight = 44.sp)
                Text(" / ${total.meta}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    color = Cores.Azul, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                Text("${(prod * 100 / total.meta.coerceAtLeast(1))}%", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFFAFC4EE),
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            BarraProgresso(prod.toFloat() / total.meta.coerceAtLeast(1),
                if (prod >= total.meta) Cores.Verde else Cores.Azul)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metrica("Meta", total.meta.toString(), Cores.Azul, Modifier.weight(1f), Color(0x22FFFFFF))
                Metrica("Aprovado", total.aprov?.toString() ?: "–", Cores.Verde, Modifier.weight(1f), Color(0x22FFFFFF))
                Metrica("Diferença", total.dif?.let(::comSinal) ?: "–", corDiferenca(total.dif),
                    Modifier.weight(1f), Color(0x22FFFFFF))
            }
            Button(
                onClick = onEnviar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cores.WhatsApp, contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Enviar fechamento no WhatsApp", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun comSinal(n: Int): String = if (n > 0) "+$n" else n.toString()

@Composable
private fun Metrica(rotulo: String, valor: String, cor: Color, modifier: Modifier = Modifier,
                    fundo: Color = Cores.SuperficieAlta) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fundo)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor, color = cor, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
        Text(rotulo, color = Cores.TextoSuave, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun Pilula(texto: String, cor: Color) {
    Text(
        texto,
        color = cor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(cor.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun EstadoVazio() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Cores.Superficie),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Cores.TextoSuave,
                modifier = Modifier.size(34.dp))
        }
        Text("Nenhum veículo ainda", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text("Toque em  +  Veículo  para registrar o primeiro VIN.", color = Cores.TextoSuave,
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
    }
}

/* ------------------------------------------------------ Card do intervalo */

@Composable
private fun CardIntervalo(
    intervalo: String,
    registros: List<Registro>,
    emAndamento: Boolean,
    selecionados: List<Long>,
    filtro: String,
    contagemVin: Map<String, Int>,
    onToque: (Registro) -> Unit,
    onToqueLongo: (Registro) -> Unit,
    onSelecionarGrupo: (List<Long>) -> Unit,
    onEnviar: () -> Unit
) {
    val doIntervalo = registros.filter { it.intervalo == intervalo }.sortedBy { it.ordem }
    val e = Formatador.estatistica(intervalo, registros)
    val emInspecao = doIntervalo.count { it.status == null }
    val ids = doIntervalo.map { it.ordem }
    val todosMarcados = ids.isNotEmpty() && selecionados.containsAll(ids)
    val modoSelecao = selecionados.isNotEmpty()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Cores.Superficie,
        border = BorderStroke(1.dp, if (emAndamento) Cores.Azul.copy(alpha = 0.6f) else Cores.Borda),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(faixaCurta(intervalo), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (emAndamento) {
                            Spacer(Modifier.width(8.dp))
                            Pilula("Agora", Cores.Azul)
                        }
                    }
                    val sub = buildString {
                        append("${doIntervalo.size} veículo(s)")
                        if (emInspecao > 0) append(" • $emInspecao em inspeção")
                    }
                    Text(sub, color = Cores.TextoSuave, style = MaterialTheme.typography.bodySmall)
                }
                Pilula(
                    e.dif?.let { if (it < 0) "Faltam ${-it}" else if (it == 0) "Meta batida" else "Meta batida +$it" } ?: "Sem produção",
                    corDiferenca(e.dif)
                )
            }

            BarraProgresso((e.prod ?: 0).toFloat() / e.meta.coerceAtLeast(1),
                if (e.prod == null) Cores.Borda else corDiferenca(e.dif), altura = 6.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metrica("Meta", e.meta.toString(), Cores.Azul, Modifier.weight(1f))
                Metrica("Produção", e.prod?.toString() ?: "–", Cores.Texto, Modifier.weight(1f))
                Metrica("Aprovado", e.aprov?.toString() ?: "–", Cores.Verde, Modifier.weight(1f))
                Metrica("Diferença", e.dif?.let(::comSinal) ?: "–", corDiferenca(e.dif), Modifier.weight(1f))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                doIntervalo.forEachIndexed { i, r ->
                    if (filtro.isEmpty() || r.vin.uppercase().contains(filtro)) {
                        LinhaVeiculo(
                            numero = i + 1,
                            r = r,
                            vezes = contagemVin[r.vin.uppercase()] ?: 1,
                            modoSelecao = modoSelecao,
                            selecionado = r.ordem in selecionados,
                            onToque = { onToque(r) },
                            onToqueLongo = { onToqueLongo(r) }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onSelecionarGrupo(ids) }) {
                    Text(if (todosMarcados) "Desmarcar todos" else "Selecionar todos")
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onEnviar,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cores.WhatsApp.copy(alpha = 0.18f),
                        contentColor = Cores.WhatsApp
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar intervalo", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LinhaVeiculo(
    numero: Int,
    r: Registro,
    vezes: Int,
    modoSelecao: Boolean,
    selecionado: Boolean,
    onToque: () -> Unit,
    onToqueLongo: () -> Unit
) {
    val corSt = corStatus(r.status)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selecionado) Cores.AzulEscuro else Cores.SuperficieAlta)
            .border(
                if (vezes > 1) 1.5.dp else 1.dp,
                when {
                    selecionado -> Cores.Azul
                    vezes > 1 -> Cores.Vermelho
                    else -> Color.Transparent
                },
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(onClick = onToque, onLongClick = onToqueLongo)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (modoSelecao) {
            Checkbox(
                checked = selecionado,
                onCheckedChange = { onToque() },
                colors = CheckboxDefaults.colors(checkedColor = Cores.Azul),
                modifier = Modifier.size(32.dp)
            )
        } else {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(corSt.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$numero", color = corSt, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(r.vin, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (vezes > 1) {
                Text("VIN repetido • aparece ${vezes}x", color = Cores.Vermelho,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
            }
            if (r.modelo != null || r.cor != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (r.cor != null) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(corDoCarro(r.cor))
                                .border(1.dp, Cores.TextoSuave.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(listOfNotNull(r.modelo, r.cor).joinToString(" • "), color = Cores.TextoSuave,
                        style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            if (r.status == Status.REPARO && !r.reparo.isNullOrBlank()) {
                Text(r.reparo, color = Cores.Ambar, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Pilula(rotuloStatus(r.status), corSt)
            if (r.status == Status.REPARO && !modoSelecao) {
                val context = LocalContext.current
                TextButton(
                    onClick = { enviarWhatsAppComFotos(context, Formatador.negado(r), r.fotos) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null,
                        tint = Cores.WhatsApp, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (r.fotos.isEmpty()) "Enviar NC" else "Enviar NC • ${r.fotos.size}📷",
                        color = Cores.WhatsApp, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MiniaturaFoto(caminho: String, onRemover: () -> Unit) {
    val bmp = remember(caminho) { Fotos.miniatura(caminho) }
    Box(
        Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Cores.SuperficieAlta)
    ) {
        if (bmp != null) {
            Image(bmp, contentDescription = "Foto do demérito", contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize())
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xAA000000))
                .combinedClickable(onClick = onRemover),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Remover foto", tint = Color.White,
                modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun BotaoFoto(icone: androidx.compose.ui.graphics.vector.ImageVector, rotulo: String, onClick: () -> Unit) {
    Column(
        Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Cores.SuperficieAlta)
            .border(1.dp, Cores.Borda, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icone, contentDescription = null, tint = Cores.Azul)
        Spacer(Modifier.height(4.dp))
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = Cores.TextoSuave)
    }
}

/* ------------------------------------------------- Cadastro / edição */

/**
 * Folha inferior de veículo. [inicial] null = novo registro (intervalo pode ser Auto);
 * senão edita o registro existente. [onSalvar] recebe o registro (ordem é ajustada por quem chama).
 */
@Composable
private fun FolhaVeiculo(
    inicial: Registro?,
    adiantar: Boolean,
    bancoReparos: List<String>,
    onFechar: () -> Unit,
    onSalvar: (Registro) -> Unit,
    onExcluir: (() -> Unit)? = null
) {
    val context = LocalContext.current
    // Todo VIN começa com "VB": o prefixo já vem escrito e o usuário digita só o resto.
    // Lido pelo código de barras: o prefixo some e o VIN fica exatamente como o código.
    var usarPrefixo by remember { mutableStateOf(inicial?.vin?.startsWith(PREFIXO_VIN) ?: true) }
    var vin by remember {
        mutableStateOf(inicial?.vin?.let { if (usarPrefixo) it.removePrefix(PREFIXO_VIN) else it } ?: "")
    }
    var modelo by remember { mutableStateOf(inicial?.modelo) }
    var cor by remember { mutableStateOf(inicial?.cor) }
    var status by remember { mutableStateOf(inicial?.status) }
    var reparo by remember { mutableStateOf(inicial?.reparo ?: "") }
    var intervalo by remember { mutableStateOf(inicial?.intervalo ?: AUTOMATICO) }
    var confirmarExcluir by remember { mutableStateOf(false) }
    var responsavel by remember { mutableStateOf(inicial?.responsavel ?: "") }
    val fotos = remember { mutableStateListOf<String>().apply { addAll(inicial?.fotos ?: emptyList()) } }
    val fotosOriginais = remember { inicial?.fotos ?: emptyList() }
    var fotoPendente by remember { mutableStateOf<File?>(null) }

    val tirarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = fotoPendente
        if (f != null) {
            if (ok && f.exists() && f.length() > 0) fotos.add(f.absolutePath) else f.delete()
        }
        fotoPendente = null
    }
    val escolherFotos = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.take(MAX_FOTOS - fotos.size).forEach { u -> Fotos.copiarDaGaleria(context, u)?.let { fotos.add(it) } }
    }

    /** Fecha sem salvar: apaga só as fotos novas desta edição. */
    fun descartar() {
        Fotos.apagar(fotos.filter { it !in fotosOriginais })
        onFechar()
    }

    fun intervaloAgora(): String {
        val c = Calendar.getInstance()
        return Intervalos.atual(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), adiantar)
    }

    /** Registro com o que está na tela (null se o VIN estiver vazio). */
    fun montar(): Registro? {
        val digitado = vin.trim()
        // Se colou o VIN completo (17 caracteres já com "VB"), não duplica o prefixo.
        val v = when {
            digitado.isBlank() -> return null
            !usarPrefixo -> digitado
            digitado.startsWith(PREFIXO_VIN) && digitado.length >= 17 -> digitado
            else -> PREFIXO_VIN + digitado
        }
        val rep = reparo.trim()
        // Reparo sem descrição = Fazendo Inspeção 360 (não contabiliza).
        val st = if (status == Status.REPARO && rep.isEmpty()) null else status
        val ehReparo = st == Status.REPARO
        return Registro(
            vin = v,
            modelo = modelo,
            cor = cor,
            status = st,
            reparo = if (ehReparo) rep else null,
            intervalo = if (intervalo == AUTOMATICO) intervaloAgora() else intervalo,
            ordem = inicial?.ordem ?: 0,
            responsavel = if (ehReparo) responsavel.trim().ifEmpty { null } else null,
            fotos = if (ehReparo) fotos.toList() else emptyList()
        )
    }

    val coresCampo = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Cores.Azul,
        unfocusedBorderColor = Cores.Borda,
        focusedContainerColor = Cores.SuperficieAlta,
        unfocusedContainerColor = Cores.SuperficieAlta
    )

    ModalBottomSheet(
        onDismissRequest = { descartar() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cores.Superficie
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(if (inicial == null) "Novo veículo" else "Editar veículo",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = vin,
                onValueChange = { vin = it.uppercase() },
                label = { Text("VIN") },
                prefix = if (usarPrefixo) {
                    { Text(PREFIXO_VIN, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        color = Cores.Azul) }
                } else null,
                leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = {
                        val opcoes = GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93,
                                Barcode.FORMAT_DATA_MATRIX, Barcode.FORMAT_QR_CODE
                            )
                            .enableAutoZoom()
                            .build()
                        GmsBarcodeScanning.getClient(context, opcoes).startScan()
                            .addOnSuccessListener { codigo ->
                                val lido = Vin.limpar(codigo.rawValue.orEmpty())
                                if (lido.isEmpty()) {
                                    Toast.makeText(context, "Código vazio, tente de novo.", Toast.LENGTH_SHORT).show()
                                } else {
                                    usarPrefixo = false
                                    vin = lido
                                    Toast.makeText(context, "VIN lido: $lido", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Não foi possível abrir o leitor de código de barras.",
                                    Toast.LENGTH_LONG).show()
                            }
                    }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Ler código de barras do chassi",
                            tint = Cores.Azul)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = coresCampo,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )

            Secao("Modelo", "opcional") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Catalogo.modelos.forEach { m ->
                        Chip(texto = m, selecionado = modelo == m,
                            onClick = { modelo = if (modelo == m) null else m })
                    }
                }
            }

            Secao("Cor", "opcional") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Catalogo.cores.forEach { c ->
                        Chip(texto = c, selecionado = cor == c, onClick = { cor = if (cor == c) null else c },
                            corBolinha = corDoCarro(c))
                    }
                }
            }

            Secao("Status", null) {
                val opcoes = listOf<Status?>(null, Status.APROVADO, Status.REPARO)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opcoes.forEachIndexed { i, st ->
                        SegmentedButton(
                            selected = status == st,
                            onClick = { status = st },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = opcoes.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = corStatus(st).copy(alpha = 0.2f),
                                activeContentColor = corStatus(st),
                                activeBorderColor = corStatus(st),
                                inactiveContainerColor = Cores.SuperficieAlta,
                                inactiveContentColor = Cores.TextoSuave,
                                inactiveBorderColor = Cores.Borda
                            ),
                            icon = {}
                        ) {
                            Text(
                                if (st == Status.APROVADO) "🟢 Aprovado" else rotuloStatus(st),
                                maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                if (status == Status.REPARO) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reparo,
                        onValueChange = { reparo = it },
                        label = { Text("Descreva o reparo") },
                        supportingText = { Text("Sem reparo escrito, o carro fica como Fazendo Inspeção 360.") },
                        shape = RoundedCornerShape(14.dp),
                        colors = coresCampo,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val sugestoes = Configuracoes.sugerir(bancoReparos, reparo)
                    if (sugestoes.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Sugestões do banco de reparos", color = Cores.TextoSuave,
                            style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sugestoes.forEach { sug ->
                                Chip(texto = sug, selecionado = false, onClick = { reparo = sug },
                                    corBolinha = Cores.Ambar)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = responsavel,
                        onValueChange = { responsavel = it },
                        label = { Text("Responsável") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = coresCampo,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RESPONSAVEIS.forEach { resp ->
                            val marcado = responsavel.trim().equals(resp, ignoreCase = true)
                            Chip(texto = resp, selecionado = marcado,
                                onClick = { responsavel = if (marcado) "" else resp })
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Fotos do demérito", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(6.dp))
                        Text("opcional • até $MAX_FOTOS", color = Cores.TextoSuave,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fotos.toList().forEach { caminho ->
                            MiniaturaFoto(caminho, onRemover = { fotos.remove(caminho) })
                        }
                        if (fotos.size < MAX_FOTOS) {
                            BotaoFoto(Icons.Filled.PhotoCamera, "Câmera") {
                                val f = Fotos.novoArquivo(context)
                                fotoPendente = f
                                tirarFoto.launch(Fotos.uriDe(context, f))
                            }
                            BotaoFoto(Icons.Filled.AddPhotoAlternate, "Galeria") {
                                escolherFotos.launch("image/*")
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val r = montar()
                            when {
                                r == null -> Toast.makeText(context, "Digite o VIN.", Toast.LENGTH_SHORT).show()
                                r.status != Status.REPARO ->
                                    Toast.makeText(context, "Escreva o reparo (NC) antes de enviar.",
                                        Toast.LENGTH_SHORT).show()
                                else -> {
                                    // Enviar também salva o veículo na lista (novo ou edição).
                                    Fotos.apagar((fotosOriginais + fotos).distinct().filter { it !in r.fotos })
                                    onSalvar(r)
                                    enviarWhatsAppComFotos(context, Formatador.negado(r), r.fotos)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Cores.WhatsApp.copy(alpha = 0.18f),
                            contentColor = Cores.WhatsApp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Salvar e enviar veículo negado (${fotos.size} foto(s))", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Secao("Intervalo", null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (inicial == null) {
                        Chip(texto = "Auto (${intervaloAgora().substring(0, 5)})",
                            selecionado = intervalo == AUTOMATICO, onClick = { intervalo = AUTOMATICO })
                    }
                    Intervalos.lista.forEach { intv ->
                        Chip(texto = intv, selecionado = intervalo == intv, onClick = { intervalo = intv })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onExcluir != null) {
                    OutlinedButton(
                        onClick = { confirmarExcluir = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Cores.Vermelho.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Cores.Vermelho),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Excluir")
                    }
                }
                Button(
                    onClick = {
                        val r = montar()
                        if (r == null) {
                            Toast.makeText(context, "Digite o VIN.", Toast.LENGTH_SHORT).show()
                        } else {
                            // Apaga as fotos que saíram (removidas, ou o carro deixou de ser Reparo).
                            Fotos.apagar((fotosOriginais + fotos).distinct().filter { it !in r.fotos })
                            onSalvar(r)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cores.Azul, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(if (inicial == null) "Adicionar veículo" else "Salvar alterações",
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (confirmarExcluir && onExcluir != null) {
        AlertDialog(
            onDismissRequest = { confirmarExcluir = false },
            containerColor = Cores.Superficie,
            title = { Text("Excluir ${inicial?.vin}?") },
            text = { Text("O veículo será removido. Não dá para desfazer.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarExcluir = false
                        Fotos.apagar(fotos.filter { it !in fotosOriginais })
                        onExcluir()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cores.Vermelho)
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExcluir = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun Secao(titulo: String, dica: String?, conteudo: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            if (dica != null) {
                Spacer(Modifier.width(6.dp))
                Text(dica, color = Cores.TextoSuave, style = MaterialTheme.typography.labelSmall)
            }
        }
        conteudo()
    }
}

@Composable
private fun Chip(texto: String, selecionado: Boolean, onClick: () -> Unit, corBolinha: Color? = null) {
    FilterChip(
        selected = selecionado,
        onClick = onClick,
        label = { Text(texto, fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Normal) },
        leadingIcon = corBolinha?.let { c ->
            {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(1.dp, Cores.TextoSuave.copy(alpha = 0.5f), CircleShape)
                )
            }
        },
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Cores.SuperficieAlta,
            labelColor = Cores.Texto,
            selectedContainerColor = Cores.AzulEscuro,
            selectedLabelColor = Color(0xFFCFE0FF)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selecionado,
            borderColor = Cores.Borda,
            selectedBorderColor = Cores.Azul
        )
    )
}

@Composable
private fun LinhaSwitch(titulo: String, descricao: String, marcado: Boolean, onMudar: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Cores.SuperficieAlta)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(descricao, style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = marcado,
            onCheckedChange = onMudar,
            colors = SwitchDefaults.colors(checkedTrackColor = Cores.Azul)
        )
    }
}
