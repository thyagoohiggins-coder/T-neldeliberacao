package com.hikins.tunelfai.ui

import android.content.ClipboardManager
import android.content.Context as AndroidContext
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.hikins.tunelfai.data.ConfigRepo
import com.hikins.tunelfai.data.EstadoTunel
import com.hikins.tunelfai.data.Repositorio
import com.hikins.tunelfai.model.Configuracao
import com.hikins.tunelfai.model.Intervalos
import com.hikins.tunelfai.model.Motivo
import com.hikins.tunelfai.model.Registro
import com.hikins.tunelfai.model.Status
import com.hikins.tunelfai.notif.Lembretes
import com.hikins.tunelfai.update.Atualizador
import com.hikins.tunelfai.update.InfoAtualizacao
import kotlinx.coroutines.launch

/** Opção do seletor de Motivo que libera o campo de texto livre. */
private const val MOTIVO_OUTRO = "Outro (escrever)"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunelApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { Repositorio(context) }
    val configRepo = remember { ConfigRepo(context) }

    val registros = remember { mutableStateListOf<Registro>() }
    val incluirMeta = remember { mutableStateMapOf<String, Boolean>() }
    var seq by remember { mutableStateOf(0L) }
    var config by remember { mutableStateOf(Configuracao()) }
    var fotoGlobal by remember { mutableStateOf<String?>(null) }

    var menuAberto by remember { mutableStateOf(false) }
    var confirmarLimpar by remember { mutableStateOf(false) }
    var mostrarConfig by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<InfoAtualizacao?>(null) }
    var mostrarDialogoUpdate by remember { mutableStateOf(false) }

    val capturarFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = java.io.File(context.cacheDir, "foto_${System.currentTimeMillis()}.png")
            file.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            try {
                context.contentResolver.takePersistableUriPermission(
                    androidx.core.content.FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    ), Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            fotoGlobal = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            ).toString()
        }
    }

    fun persistir() {
        repo.salvar(EstadoTunel(registros.toList(), seq))
    }

    fun aplicarConfig(nova: Configuracao) {
        config = nova
        configRepo.salvar(nova)
        if (nova.vibracaoAtivada) Lembretes.agendar(context, nova.vibracaoSegundos)
        else Lembretes.cancelar(context)
    }

    LaunchedEffect(Unit) {
        val e = repo.carregar()
        registros.addAll(e.registros)
        seq = e.seq
        config = configRepo.carregar()
        if (config.vibracaoAtivada) Lembretes.agendar(context, config.vibracaoSegundos)
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
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
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
                FormularioRegistro(
                    config = config,
                    onAdicionar = { vin, status, motivo, motivoTexto, intervalo, fotos ->
                        seq += 1
                        registros.add(Registro(vin, status, motivo, motivoTexto, intervalo, seq, fotos))
                        persistir()
                        Toast.makeText(context, "Veículo adicionado.", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Intervalos presentes, em ordem decrescente (15:00 no topo)
            val intervalosPresentes = registros.map { it.intervalo }.distinct().sortedDescending()
            items(intervalosPresentes, key = { it }) { intervalo ->
                GrupoIntervalo(
                    intervalo = intervalo,
                    registros = registros,
                    incluirMeta = incluirMeta,
                    config = config,
                    onMudou = { persistir() }
                )
            }

            // Resumo total
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Resumo total", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(0.dp))
                        Text(
                            Formatador.total(registros, config).replace("*", ""),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Button(
                            onClick = { enviarWhatsApp(context, Formatador.total(registros, config)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Formatar total e enviar")
                        }
                        Button(
                            onClick = {
                                val texto = Formatador.todosAgrupados(registros, config, incluirMeta)
                                if (texto.isBlank())
                                    Toast.makeText(context, "Nenhum veículo registrado.", Toast.LENGTH_SHORT).show()
                                else enviarWhatsApp(context, texto, registros.flatMap { it.fotos })
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Enviar todos os intervalos (agrupado)")
                        }
                        Spacer(Modifier.width(0.dp))
                        OutlinedButton(
                            onClick = {
                                val texto = Formatador.negadosDeTodos(registros, config)
                                if (texto.isBlank())
                                    Toast.makeText(context, "Nenhum VIN negado.", Toast.LENGTH_SHORT).show()
                                else enviarWhatsApp(context, texto)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Enviar só negados (todos)")
                        }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        Text("Enviar imagem", fontWeight = FontWeight.Bold, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { capturarFoto.launch(null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Câmera")
                            }
                            if (fotoGlobal != null) {
                                Button(
                                    onClick = { enviarWhatsApp(context, "", listOf(fotoGlobal!!)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Enviar")
                                }
                                OutlinedButton(
                                    onClick = { fotoGlobal = null },
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Text("Limpar")
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.width(0.dp)) }
        }
    }

    if (mostrarConfig) {
        ConfiguracoesDialog(
            config = config,
            onSalvar = { aplicarConfig(it); mostrarConfig = false },
            onDismiss = { mostrarConfig = false }
        )
    }

    if (confirmarLimpar) {
        AlertDialog(
            onDismissRequest = { confirmarLimpar = false },
            title = { Text("Limpar tudo?") },
            text = { Text("Isso remove todos os veículos e estatísticas do dia. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    registros.clear(); incluirMeta.clear(); seq = 0
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfiguracoesDialog(
    config: Configuracao,
    onSalvar: (Configuracao) -> Unit,
    onDismiss: () -> Unit
) {
    var exigirFoto by remember { mutableStateOf(config.exigirFoto) }
    var motivoAtivado by remember { mutableStateOf(config.motivoAtivado) }
    var motivoLivre by remember { mutableStateOf(config.motivoLivre) }
    var vibracaoAtivada by remember { mutableStateOf(config.vibracaoAtivada) }
    var vibracaoSegundos by remember { mutableStateOf(config.vibracaoSegundos.toString()) }
    var destacarNegrito by remember { mutableStateOf(config.destacarNegrito) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinhaSwitch("Exigir foto por VIN para adicionar", exigirFoto) { exigirFoto = it }
                LinhaSwitch("Ativar motivo do VIN negado", motivoAtivado) { motivoAtivado = it }
                LinhaSwitch("Escrever o motivo à mão", motivoLivre) { motivoLivre = it }
                LinhaSwitch("Destacar prefixos em negrito", destacarNegrito) { destacarNegrito = it }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                LinhaSwitch("Lembrete + vibração antes do intervalo", vibracaoAtivada) { vibracaoAtivada = it }
                OutlinedTextField(
                    value = vibracaoSegundos,
                    onValueChange = { vibracaoSegundos = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Segundos de vibração") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "O lembrete \"Fazer Relatório\" vibra 10 minutos antes de cada intervalo começar.",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSalvar(
                    Configuracao(
                        exigirFoto = exigirFoto,
                        motivoAtivado = motivoAtivado,
                        motivoLivre = motivoLivre,
                        vibracaoAtivada = vibracaoAtivada,
                        vibracaoSegundos = (vibracaoSegundos.toIntOrNull() ?: 3).coerceIn(1, 60),
                        destacarNegrito = destacarNegrito
                    )
                )
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun LinhaSwitch(rotulo: String, valor: Boolean, onMudar: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rotulo, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onMudar)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioRegistro(
    config: Configuracao,
    onAdicionar: (String, Status, Motivo?, String?, String, List<String>) -> Unit
) {
    val context = LocalContext.current
    var vin by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(Status.LIBERADO) }
    var motivoSel by remember { mutableStateOf(Motivo.DEMERITO_ABERTO.texto) }
    var motivoTexto by remember { mutableStateOf("") }
    // Já seleciona o intervalo pela hora do celular (ex.: 13:45 -> "13:00").
    var intervalo by remember {
        mutableStateOf<String?>(
            Intervalos.paraHora(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY))
        )
    }
    val fotos = remember { mutableStateListOf<String>() }

    val seletorFotos = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            if (fotos.size < 3) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
                fotos.add(uri.toString())
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Registrar veículo", fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = vin,
                onValueChange = { vin = it.uppercase() },
                label = { Text("VIN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )

            // Status
            DropdownSelecao(
                rotulo = "Status",
                valor = status.texto,
                opcoes = Status.entries.map { it.texto },
                onSelecionar = { txt -> status = Status.entries.first { it.texto == txt } }
            )

            // Motivo só aparece quando Negado e ativado nas configurações
            if (status == Status.NEGADO && config.motivoAtivado) {
                if (config.motivoLivre) {
                    // Configuração "escrever à mão": só campo de texto.
                    OutlinedTextField(
                        value = motivoTexto,
                        onValueChange = { motivoTexto = it },
                        label = { Text("Motivo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Lista de motivos + opção "Outro (escrever)" para digitar.
                    DropdownSelecao(
                        rotulo = "Motivo",
                        valor = motivoSel,
                        opcoes = Motivo.entries.map { it.texto } + MOTIVO_OUTRO,
                        onSelecionar = { motivoSel = it }
                    )
                    if (motivoSel == MOTIVO_OUTRO) {
                        OutlinedTextField(
                            value = motivoTexto,
                            onValueChange = { motivoTexto = it },
                            label = { Text("Escreva o motivo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Intervalo
            DropdownSelecao(
                rotulo = "Intervalo",
                valor = intervalo ?: "Selecione",
                opcoes = Intervalos.lista,
                onSelecionar = { intervalo = it }
            )

            // Fotos (até 3 por VIN)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { seletorFotos.launch(arrayOf("image/*")) },
                    enabled = fotos.size < 3,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Fotos ${fotos.size}/3")
                }
                if (fotos.isNotEmpty()) {
                    TextButton(onClick = { fotos.clear() }) { Text("Limpar fotos") }
                }
            }

            Button(
                onClick = {
                    val v = vin.trim()
                    when {
                        v.isBlank() -> Toast.makeText(context, "Digite o VIN.", Toast.LENGTH_SHORT).show()
                        intervalo == null -> Toast.makeText(context, "Escolha o intervalo.", Toast.LENGTH_SHORT).show()
                        config.exigirFoto && fotos.isEmpty() ->
                            Toast.makeText(context, "Anexe ao menos uma foto do VIN.", Toast.LENGTH_LONG).show()
                        else -> {
                            val usarMotivo = status == Status.NEGADO && config.motivoAtivado
                            val escrever = usarMotivo && (config.motivoLivre || motivoSel == MOTIVO_OUTRO)
                            val mEnum = if (usarMotivo && !escrever)
                                Motivo.entries.first { it.texto == motivoSel } else null
                            val mTexto = if (escrever) motivoTexto.trim().ifBlank { null } else null
                            onAdicionar(v, status, mEnum, mTexto, intervalo!!, fotos.toList())
                            vin = ""
                            motivoTexto = ""
                            motivoSel = Motivo.DEMERITO_ABERTO.texto
                            fotos.clear()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Adicionar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrupoIntervalo(
    intervalo: String,
    registros: SnapshotStateList<Registro>,
    incluirMeta: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    config: Configuracao,
    onMudou: () -> Unit
) {
    val context = LocalContext.current
    val doIntervalo = registros.filter { it.intervalo == intervalo }.sortedBy { it.ordem }
    val incluir = incluirMeta[intervalo] ?: true
    var editando by remember { mutableStateOf<Registro?>(null) }

    val prod = doIntervalo.size
    val aprov = doIntervalo.count { it.status == Status.LIBERADO }
    val dif = prod - Intervalos.metaDe(intervalo)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Intervalo $intervalo", fontWeight = FontWeight.Bold)
                Text("${doIntervalo.size} veículo(s) • Meta ${Intervalos.metaDe(intervalo)}")
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            doIntervalo.forEachIndexed { i, r ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val motivoTxt = if (r.status == Status.NEGADO && config.motivoAtivado)
                        r.motivoExibido()?.let { " — $it" } ?: "" else ""
                    val fotoTxt = if (r.fotos.isNotEmpty()) " 📷${r.fotos.size}" else ""
                    Text(
                        "${i + 1}. ${r.vin}  [${r.status.texto}$motivoTxt]$fotoTxt",
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Monospace,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                    IconButton(onClick = { editando = r }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { registros.remove(r); onMudou() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                    }
                }
            }

            editando?.let { alvo ->
                EditarRegistroDialog(
                    registro = alvo,
                    config = config,
                    onSalvar = { novo ->
                        val idx = registros.indexOfFirst { it.ordem == alvo.ordem }
                        if (idx >= 0) registros[idx] = novo
                        editando = null
                        onMudou()
                    },
                    onDismiss = { editando = null }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // Contagem automática: Prod / Aprov / Dif
            Text(
                "Prod: $prod   Aprov: $aprov   Dif: $dif",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = incluir,
                    onCheckedChange = { incluirMeta[intervalo] = it }
                )
                Text("Incluir linha da meta na mensagem")
            }

            Button(
                onClick = {
                    val texto = Formatador.intervalo(intervalo, doIntervalo, incluir, config)
                    enviarWhatsApp(context, texto)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Formatar intervalo e enviar")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val texto = Formatador.intervalo(intervalo, doIntervalo, incluir, config, somenteNegado = true)
                        if (texto.isBlank())
                            Toast.makeText(context, "Nenhum VIN negado neste intervalo.", Toast.LENGTH_SHORT).show()
                        else enviarWhatsApp(context, texto)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Enviar negados")
                }
                OutlinedButton(
                    onClick = {
                        val texto = Formatador.intervalo(intervalo, doIntervalo, incluir, config)
                        val cm = context.getSystemService(AndroidContext.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = android.content.ClipData.newPlainText("intervalo_$intervalo", texto)
                        cm.setPrimaryClip(clip)
                        Toast.makeText(context, "Copiado para a área de transferência.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarRegistroDialog(
    registro: Registro,
    config: Configuracao,
    onSalvar: (Registro) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var vin by remember { mutableStateOf(registro.vin) }
    var status by remember { mutableStateOf(registro.status) }
    var intervalo by remember { mutableStateOf(registro.intervalo) }
    val temTextoLivre = !registro.motivoTexto.isNullOrBlank()
    var motivoSel by remember {
        mutableStateOf(
            when {
                temTextoLivre -> MOTIVO_OUTRO
                registro.motivo != null -> registro.motivo.texto
                else -> Motivo.DEMERITO_ABERTO.texto
            }
        )
    }
    var motivoTexto by remember { mutableStateOf(registro.motivoTexto ?: "") }
    val fotos = remember { mutableStateListOf<String>().apply { addAll(registro.fotos) } }

    val seletorFotos = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            if (fotos.size < 3) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
                fotos.add(uri.toString())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar veículo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = vin,
                    onValueChange = { vin = it.uppercase() },
                    label = { Text("VIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownSelecao(
                    rotulo = "Status",
                    valor = status.texto,
                    opcoes = Status.entries.map { it.texto },
                    onSelecionar = { txt -> status = Status.entries.first { it.texto == txt } }
                )
                if (status == Status.NEGADO && config.motivoAtivado) {
                    DropdownSelecao(
                        rotulo = "Motivo",
                        valor = motivoSel,
                        opcoes = Motivo.entries.map { it.texto } + MOTIVO_OUTRO,
                        onSelecionar = { motivoSel = it }
                    )
                    if (motivoSel == MOTIVO_OUTRO) {
                        OutlinedTextField(
                            value = motivoTexto,
                            onValueChange = { motivoTexto = it },
                            label = { Text("Escreva o motivo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                DropdownSelecao(
                    rotulo = "Intervalo",
                    valor = intervalo,
                    opcoes = Intervalos.lista,
                    onSelecionar = { intervalo = it }
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { seletorFotos.launch(arrayOf("image/*")) },
                        enabled = fotos.size < 3,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Fotos ${fotos.size}/3")
                    }
                    if (fotos.isNotEmpty()) {
                        TextButton(onClick = { fotos.clear() }) { Text("Limpar") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = vin.trim()
                if (v.isBlank()) {
                    Toast.makeText(context, "Digite o VIN.", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                val usarMotivo = status == Status.NEGADO && config.motivoAtivado
                val escrever = usarMotivo && (config.motivoLivre || motivoSel == MOTIVO_OUTRO)
                val mEnum = if (usarMotivo && !escrever)
                    Motivo.entries.first { it.texto == motivoSel } else null
                val mTexto = if (escrever) motivoTexto.trim().ifBlank { null } else null
                onSalvar(
                    registro.copy(
                        vin = v,
                        status = status,
                        motivo = mEnum,
                        motivoTexto = mTexto,
                        intervalo = intervalo,
                        fotos = fotos.toList()
                    )
                )
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
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
