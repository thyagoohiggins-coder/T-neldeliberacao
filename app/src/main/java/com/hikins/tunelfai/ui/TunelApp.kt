package com.hikins.tunelfai.ui

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import com.hikins.tunelfai.data.EstadoTunel
import com.hikins.tunelfai.data.MetaInput
import com.hikins.tunelfai.data.Repositorio
import com.hikins.tunelfai.model.Intervalos
import com.hikins.tunelfai.model.Motivo
import com.hikins.tunelfai.model.Registro
import com.hikins.tunelfai.model.Status
import com.hikins.tunelfai.update.Atualizador
import com.hikins.tunelfai.update.InfoAtualizacao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunelApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { Repositorio(context) }

    val registros = remember { mutableStateListOf<Registro>() }
    val metas = remember { mutableStateMapOf<String, MetaInput>() }
    val incluirMeta = remember { mutableStateMapOf<String, Boolean>() }
    var seq by remember { mutableStateOf(0L) }

    var menuAberto by remember { mutableStateOf(false) }
    var confirmarLimpar by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<InfoAtualizacao?>(null) }
    var mostrarDialogoUpdate by remember { mutableStateOf(false) }

    fun persistir() {
        repo.salvar(EstadoTunel(registros.toList(), metas.toMap(), seq))
    }

    LaunchedEffect(Unit) {
        val e = repo.carregar()
        registros.addAll(e.registros)
        metas.putAll(e.metas)
        seq = e.seq
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
                    onAdicionar = { vin, status, motivo, intervalo ->
                        seq += 1
                        registros.add(Registro(vin, status, motivo, intervalo, seq))
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
                    metas = metas,
                    incluirMeta = incluirMeta,
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
                            Formatador.total(registros),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
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

    if (confirmarLimpar) {
        AlertDialog(
            onDismissRequest = { confirmarLimpar = false },
            title = { Text("Limpar tudo?") },
            text = { Text("Isso remove todos os veículos e estatísticas do dia. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    registros.clear(); metas.clear(); incluirMeta.clear(); seq = 0
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
private fun FormularioRegistro(
    onAdicionar: (String, Status, Motivo?, String) -> Unit
) {
    val context = LocalContext.current
    var vin by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(Status.LIBERADO) }
    var motivo by remember { mutableStateOf(Motivo.DEMERITO_ABERTO) }
    var intervalo by remember { mutableStateOf<String?>(null) }

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

            // Motivo só aparece quando Negado
            if (status == Status.NEGADO) {
                DropdownSelecao(
                    rotulo = "Motivo",
                    valor = motivo.texto,
                    opcoes = Motivo.entries.map { it.texto },
                    onSelecionar = { txt -> motivo = Motivo.entries.first { it.texto == txt } }
                )
            }

            // Intervalo
            DropdownSelecao(
                rotulo = "Intervalo",
                valor = intervalo ?: "Selecione",
                opcoes = Intervalos.lista,
                onSelecionar = { intervalo = it }
            )

            Button(
                onClick = {
                    val v = vin.trim()
                    when {
                        v.isBlank() -> Toast.makeText(context, "Digite o VIN.", Toast.LENGTH_SHORT).show()
                        intervalo == null -> Toast.makeText(context, "Escolha o intervalo.", Toast.LENGTH_SHORT).show()
                        else -> {
                            val m = if (status == Status.NEGADO) motivo else null
                            onAdicionar(v, status, m, intervalo!!)
                            vin = ""
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
    metas: SnapshotStateMap<String, MetaInput>,
    incluirMeta: SnapshotStateMap<String, Boolean>,
    onMudou: () -> Unit
) {
    val context = LocalContext.current
    val doIntervalo = registros.filter { it.intervalo == intervalo }.sortedBy { it.ordem }
    val meta = metas[intervalo] ?: MetaInput()
    val incluir = incluirMeta[intervalo] ?: true

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
                    val motivoTxt = if (r.status == Status.NEGADO && r.motivo != null)
                        " — ${r.motivo.texto}" else ""
                    Text(
                        "${i + 1}. ${r.vin}  [${r.status.texto}$motivoTxt]",
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Monospace,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                    IconButton(onClick = { registros.remove(r); onMudou() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // Prod / Aprov digitados e Dif calculada
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = meta.prod,
                    onValueChange = { metas[intervalo] = meta.copy(prod = it.filter { c -> c.isDigit() }); onMudou() },
                    label = { Text("Prod") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = meta.aprov,
                    onValueChange = { metas[intervalo] = meta.copy(aprov = it.filter { c -> c.isDigit() }); onMudou() },
                    label = { Text("Aprov") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val prod = meta.prod.toIntOrNull() ?: 0
                    Text("Dif: ${Intervalos.metaDe(intervalo) - prod}", fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = incluir,
                    onCheckedChange = { incluirMeta[intervalo] = it }
                )
                Text("Incluir linha da meta na mensagem")
            }

            Button(
                onClick = {
                    val texto = Formatador.intervalo(intervalo, doIntervalo, incluir, meta)
                    enviarWhatsApp(context, texto)
                },
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
