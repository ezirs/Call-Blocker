package com.ezirs

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import com.ezirs.BuildConfig
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ezirs.data.CallRule
import com.ezirs.data.RuleType
import com.ezirs.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "id") ?: "id"
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val newContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(newContext)
    }



    private val viewModel: MainViewModel by viewModels()

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, getString(R.string.msg_screening_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.msg_screening_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissionsToRequest = mutableListOf(
            android.Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
        
        enableEdgeToEdge()

        setContent {
            
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
            val themePref = prefs.getInt("theme_pref", 0) // 0: System, 1: Light, 2: Dark
            val isDarkTheme = when (themePref) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                var currentTab by remember { mutableIntStateOf(0) }
                // Pre-fill states
                var newRuleValue by remember { mutableStateOf("") }
                var newRuleType by remember { mutableStateOf(RuleType.AWALAN) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),

                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.tab_filter)) },
                                label = { Text(stringResource(R.string.tab_filter)) },
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.tab_blocked)) },
                                label = { Text(stringResource(R.string.tab_blocked)) },
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.tab_passed)) },
                                label = { Text(stringResource(R.string.tab_passed)) },
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.tab_whitelist)) }, // Change icon later if needed
                                label = { Text(stringResource(R.string.tab_whitelist)) },
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 }
                            )

                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        0 -> CallBlockerScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            newRuleValue = newRuleValue,
                            newRuleType = newRuleType,
                            onValueChange = { newRuleValue = it },
                            onTypeChange = { newRuleType = it },
                            onRequestRole = { requestCallScreeningRole() },
                            onOpenSettings = { currentTab = 4 }
                        )
                        1 -> LogsScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            isBlockedLogs = true,
                            onMakeRule = { type, number ->
                                newRuleType = type
                                newRuleValue = number
                                currentTab = 0
                            }
                        )
                        2 -> LogsScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            isBlockedLogs = false,
                            onMakeRule = { type, number ->
                                newRuleType = type
                                newRuleValue = number
                                currentTab = 0
                            }
                        )
                        3 -> WhitelistScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel
                        )
                        4 -> SettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                try {
                    roleRequestLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.msg_cannot_open_settings), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.msg_already_default), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.msg_require_android_10), Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallBlockerScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    newRuleValue: String,
    newRuleType: RuleType,
    onValueChange: (String) -> Unit,
    onTypeChange: (RuleType) -> Unit,
    onRequestRole: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val items by viewModel.rules.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isRoleGranted by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    isRoleGranted = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showTypeDropdown by remember { mutableStateOf(false) }
    var contactMatches by remember { mutableStateOf<List<ContactMatch>?>(null) }
    var selectedContactsForWhitelist by remember { mutableStateOf<Set<String>>(emptySet()) }
    val whitelists by viewModel.whitelists.collectAsStateWithLifecycle()

    contactMatches?.let { matches ->
        AlertDialog(
            onDismissRequest = { 
                contactMatches = null 
                selectedContactsForWhitelist = emptySet()
            },
            title = { Text(stringResource(R.string.title_contact_warning)) },
            text = { 
                Column {
                    Text(stringResource(R.string.msg_contact_warning, matches.size))
                    Spacer(modifier = Modifier.height(8.dp))
                    val dialogListState1 = rememberLazyListState()
                    LazyColumn(
                        state = dialogListState1,
                        modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).simpleVerticalScrollbar(dialogListState1).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(matches) { match ->
                            val isChecked = selectedContactsForWhitelist.contains(match.number)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedContactsForWhitelist = if (isChecked) {
                                            selectedContactsForWhitelist - match.number
                                        } else {
                                            selectedContactsForWhitelist + match.number
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedContactsForWhitelist = if (checked) {
                                            selectedContactsForWhitelist + match.number
                                        } else {
                                            selectedContactsForWhitelist - match.number
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(match.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(match.number, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.msg_confirm_add_rule))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    matches.filter { selectedContactsForWhitelist.contains(it.number) }.forEach { match ->
                        viewModel.addWhitelist(com.ezirs.data.WhitelistNumber(name = match.name, number = match.number))
                    }
                    contactMatches = null
                    selectedContactsForWhitelist = emptySet()
                    viewModel.addRule(CallRule(type = newRuleType, value = newRuleValue))
                    onValueChange("")
                }) { Text(stringResource(R.string.btn_add_anyway), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    contactMatches = null 
                    selectedContactsForWhitelist = emptySet()
                }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Call Screening Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val uriHandler = LocalUriHandler.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "v" + BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "GitHub",
                        style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { uriHandler.openUri("https://github.com/ezirs") }
                    )
                    Text(
                        text = "Donate",
                        style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { uriHandler.openUri("https://trakteer.id/ezirs/tip") }
                    )
                }
            }
            
            // Active Pill
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.title_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .clickable {
                            if (isRoleGranted) {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                    context.startActivity(intent)
                                    Toast.makeText(context, context.getString(R.string.msg_remove_from_settings), Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.msg_failed_open_settings), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                onRequestRole()
                            }
                        }
                        .background(
                            if (isRoleGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, 
                            RoundedCornerShape(percent = 50)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                Text(
                    text = if (isRoleGranted) stringResource(R.string.lbl_active) else stringResource(R.string.lbl_inactive),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isRoleGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 24.dp)
                        .background(
                            if (isRoleGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, 
                            RoundedCornerShape(percent = 50)
                        )
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                            .align(if (isRoleGranted) Alignment.CenterEnd else Alignment.CenterStart)
                    )
                }
            }
            }
        }

        // Main Content (Rules)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFD0BCFF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings, 
                                contentDescription = stringResource(R.string.cd_rules),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Column {
                            Text(
                                text = "Smart Blocking Rules",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Automated Call Screening Service",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Active Rules List
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .simpleVerticalScrollbar(listState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(rule.type.getStringRes()).uppercase() + " FILTER",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = rule.value,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteRule(rule) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Rule",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        
                        if (items.isEmpty()) {
                            item {
                                Text(
                                    "No rules added yet.", 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Add Rule Form
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { showTypeDropdown = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    Text(stringResource(newRuleType.getStringRes()).take(7), style = MaterialTheme.typography.labelMedium)
                                }
                                DropdownMenu(expanded = showTypeDropdown, onDismissRequest = { showTypeDropdown = false }) {
                                    RuleType.values().forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(type.getStringRes())) },
                                            onClick = { onTypeChange(type); showTypeDropdown = false }
                                        )
                                    }
                                }
                            }

                            TextField(
                                value = newRuleValue,
                                onValueChange = onValueChange,
                                modifier = Modifier
                                    .weight(2f)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.placeholder_value), style = MaterialTheme.typography.bodyMedium) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                            )
                        }

                        Button(
                            onClick = {
                                if (newRuleValue.isNotBlank()) {
                                    // Check contacts
                                    val contactMatchList = checkContactsMatch(context, newRuleType, newRuleValue, whitelists)
                                    if (contactMatchList.isNotEmpty()) {
                                        contactMatches = contactMatchList
                                    } else {
                                        viewModel.addRule(CallRule(type = newRuleType, value = newRuleValue))
                                        onValueChange("")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(stringResource(R.string.btn_add_new_rule), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

            if (!isRoleGranted) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    Button(
                        onClick = onRequestRole,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(stringResource(R.string.btn_set_default), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    Text(
                        text = "Info: Jadikan Default Caller ID & Spam app untuk memblokir panggilan.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }

fun formatLogDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

fun formatPhoneNumberInfo(number: String): String {
    val clean = number.filter { it.isDigit() || it == '+' }
    if (clean.startsWith("+62") && clean.length > 5) {
        val cc = clean.substring(0, 3) 
        val op = clean.substring(3, Math.min(6, clean.length)) 
        val firstBlock = if (clean.length > 6) clean.substring(6, Math.min(10, clean.length)) else ""
        val secondBlock = if (clean.length > 10) clean.substring(10) else ""
        return listOf(cc, op, firstBlock, secondBlock).filter { it.isNotEmpty() }.joinToString("-")
    } else if ((clean.startsWith("08") || clean.startsWith("62")) && clean.length > 4) {
        return clean.chunked(4).joinToString("-")
    }
    return number // if no format matched
}

@Composable
fun LogsScreen(modifier: Modifier = Modifier, viewModel: MainViewModel, isBlockedLogs: Boolean, onMakeRule: (RuleType, String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    
    val allLogs by viewModel.logs.collectAsStateWithLifecycle()
    val logs = allLogs.filter { it.isBlocked == isBlockedLogs }

    var sortOption by remember { mutableIntStateOf(0) }
    var expandedSort by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var groupByRule by remember { mutableStateOf(prefs.getBoolean("group_by_rule_$isBlockedLogs", false)) }
    val expandedGroups = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(isBlockedLogs) {
        sortOption = 0
    }

    val sortedLogs = remember(logs, sortOption) {
        when (sortOption) {
            1 -> logs.sortedBy { it.timestamp }
            2 -> logs.sortedWith(Comparator { a, b ->
                val numA = a.phoneNumber.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                val numB = b.phoneNumber.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                numA.compareTo(numB)
            })
            3 -> logs.sortedWith(Comparator { a, b ->
                val numA = a.phoneNumber.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                val numB = b.phoneNumber.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                numB.compareTo(numA)
            })
            else -> logs.sortedByDescending { it.timestamp }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.title_delete_all_logs)) },
            text = { Text(stringResource(R.string.msg_confirm_delete_all_logs, if (isBlockedLogs) stringResource(R.string.type_blocked) else stringResource(R.string.type_passed))) },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.deleteLogsByBlockedStatus(isBlockedLogs)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBlockedLogs) stringResource(R.string.title_blocked_logs) else stringResource(R.string.title_passed_logs),
                style = MaterialTheme.typography.titleLarge
            )
            if (logs.isNotEmpty()) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus Semua", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        // Sorting Dropdown and Grouping Toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Button(
                    onClick = { expandedSort = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (sortOption) {
                            1 -> stringResource(R.string.lbl_sort_oldest)
                            2 -> stringResource(R.string.lbl_sort_smallest)
                            3 -> stringResource(R.string.lbl_sort_largest)
                            else -> stringResource(R.string.lbl_sort_newest)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_newest)) }, onClick = { sortOption = 0; expandedSort = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_oldest)) }, onClick = { sortOption = 1; expandedSort = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_smallest)) }, onClick = { sortOption = 2; expandedSort = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_largest)) }, onClick = { sortOption = 3; expandedSort = false })
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.lbl_group_rule), style = MaterialTheme.typography.labelMedium)
                Switch(
                    checked = groupByRule,
                    onCheckedChange = { 
                        groupByRule = it
                        prefs.edit().putBoolean("group_by_rule_$isBlockedLogs", it).apply()
                    }
                )
            }
        }

        val logsListState = rememberLazyListState()
        LazyColumn(
            state = logsListState,
            modifier = Modifier.simpleVerticalScrollbar(logsListState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (groupByRule) {
                // Determine order of groups based on the sortedLogs
                val reasonOrder = sortedLogs.map { it.reason }.distinct()
                val groupedLogs = sortedLogs.groupBy { it.reason }
                
                reasonOrder.forEach { reason ->
                    val groupList = groupedLogs[reason] ?: emptyList()
                    val isExpanded = expandedGroups[reason] ?: false
                    item(key = "header_${isBlockedLogs}_$reason") {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp).clickable { 
                                expandedGroups[reason] = !isExpanded 
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$reason (${groupList.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    imageVector = if (isExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) stringResource(R.string.cd_close) else stringResource(R.string.cd_open),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    if (isExpanded) {
                        items(groupList, key = { "grouped_${isBlockedLogs}_${it.id}" }) { log ->
                            LogItem(log = log, viewModel = viewModel, onMakeRule = onMakeRule)
                        }
                    }
                }
            } else {
                items(sortedLogs, key = { "flat_${isBlockedLogs}_${it.id}" }) { log ->
                    LogItem(log = log, viewModel = viewModel, onMakeRule = onMakeRule)
                }
            }
            if (sortedLogs.isEmpty()) {
                item { Text(stringResource(R.string.msg_no_logs), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
fun LogItem(
    log: com.ezirs.data.CallLogEntry,
    viewModel: MainViewModel,
    onMakeRule: (RuleType, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = formatPhoneNumberInfo(log.phoneNumber), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                    Text(text = formatLogDate(log.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = { viewModel.deleteLog(log) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_delete), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = log.reason, style = MaterialTheme.typography.bodySmall, color = if (log.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = {
                        val fullCleanNum = log.phoneNumber.filter { it.isDigit() || it == '+' }
                        onMakeRule(RuleType.AWALAN, fullCleanNum)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(stringResource(R.string.btn_prefix_rule), fontSize = 12.sp)
                }
                TextButton(
                    onClick = {
                        val fullCleanNum = log.phoneNumber.filter { it.isDigit() || it == '+' }
                        onMakeRule(RuleType.TEPAT, fullCleanNum)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(stringResource(R.string.btn_exact_match), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    
    var themePref by remember { mutableIntStateOf(prefs.getInt("theme_pref", 0)) }
    var langPref by remember { mutableStateOf(prefs.getString("language", "id") ?: "id") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val dbFile = context.getDatabasePath("call_blocker_database")
                    if (dbFile.exists()) {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            dbFile.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.msg_export_success), Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.msg_export_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val dbFile = context.getDatabasePath("call_blocker_database")
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        dbFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    (context as? android.app.Activity)?.recreate()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.msg_restore_success), Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.msg_restore_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(stringResource(R.string.title_settings), style = MaterialTheme.typography.titleLarge)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.lbl_theme), style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { 
                            themePref = 0
                            prefs.edit().putInt("theme_pref", 0).apply()
                            (context as? android.app.Activity)?.recreate()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (themePref == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) { Text(stringResource(R.string.theme_system), color = if (themePref == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                    Button(
                        onClick = { 
                            themePref = 1
                            prefs.edit().putInt("theme_pref", 1).apply()
                            (context as? android.app.Activity)?.recreate()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (themePref == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) { Text(stringResource(R.string.theme_light), color = if (themePref == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                    Button(
                        onClick = { 
                            themePref = 2
                            prefs.edit().putInt("theme_pref", 2).apply()
                            (context as? android.app.Activity)?.recreate()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (themePref == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) { Text(stringResource(R.string.theme_dark), color = if (themePref == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.lbl_language), style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { 
                            if (langPref != "id") {
                                langPref = "id"
                                prefs.edit().putString("language", "id").apply()
                                (context as? android.app.Activity)?.recreate()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (langPref == "id") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) { Text("Indonesia", color = if (langPref == "id") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                    Button(
                        onClick = { 
                            if (langPref != "en") {
                                langPref = "en"
                                prefs.edit().putString("language", "en").apply()
                                (context as? android.app.Activity)?.recreate()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (langPref == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) { Text("English", color = if (langPref == "en") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                }
            }
        }

        val dbFile = context.getDatabasePath("call_blocker_database")
        val dbSize = if (dbFile.exists()) "${dbFile.length() / 1024} KB" else "0 KB"
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.title_db_management), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.lbl_main_db), style = MaterialTheme.typography.labelLarge)
                    Text(dbSize, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.desc_json_backup), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Button(
                    onClick = { exportLauncher.launch("call_blocker_backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.btn_export_json))
                }
                
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.btn_import_json))
                }
            }
        }
    }
}

@Composable
fun WhitelistScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val whitelists by viewModel.whitelists.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf("") }
    val context = LocalContext.current
    var allContacts by remember { mutableStateOf<List<ContactMatch>>(emptyList()) }
    var selectedContactsForWhitelist by remember { mutableStateOf<Set<String>>(emptySet()) }
    var contactSearchQuery by remember { mutableStateOf("") }

    if (showContactDialog) {
        LaunchedEffect(Unit) {
            contactSearchQuery = ""
            allContacts = getAllContacts(context).filter { contact -> 
                whitelists.none { w -> 
                    val num = contact.number.replace(" ", "").replace("-", "")
                    val cleanNum = if (num.startsWith("+62")) num.substring(3) 
                               else if (num.startsWith("62")) num.substring(2) 
                               else if (num.startsWith("0")) num.substring(1) else num
                    val wNum = w.number.replace(" ", "").replace("-", "")
                    val cleanWNum = if (wNum.startsWith("+62")) wNum.substring(3)
                                    else if (wNum.startsWith("62")) wNum.substring(2)
                                    else if (wNum.startsWith("0")) wNum.substring(1) else wNum
                    num == wNum || cleanNum == cleanWNum || num.endsWith(cleanWNum) || cleanNum.endsWith(cleanWNum)
                }
            }
        }
        AlertDialog(
            onDismissRequest = { 
                showContactDialog = false 
                selectedContactsForWhitelist = emptySet()
            },
            title = { Text(stringResource(R.string.title_select_contact)) },
            text = {
                Column {
                    Text(stringResource(R.string.desc_select_contact))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = contactSearchQuery,
                        onValueChange = { contactSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.placeholder_search)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val filteredContacts = remember(allContacts, contactSearchQuery) {
                        if (contactSearchQuery.isBlank()) {
                            allContacts
                        } else {
                            allContacts.filter {
                                it.name.contains(contactSearchQuery, ignoreCase = true) || 
                                it.number.contains(contactSearchQuery)
                            }
                        }
                    }
                    val allFilteredSelected = filteredContacts.isNotEmpty() && filteredContacts.all { selectedContactsForWhitelist.contains(it.number) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (allFilteredSelected) {
                                    selectedContactsForWhitelist = selectedContactsForWhitelist - filteredContacts.map { it.number }.toSet()
                                } else {
                                    selectedContactsForWhitelist = selectedContactsForWhitelist + filteredContacts.map { it.number }.toSet()
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allFilteredSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedContactsForWhitelist = selectedContactsForWhitelist + filteredContacts.map { it.number }.toSet()
                                } else {
                                    selectedContactsForWhitelist = selectedContactsForWhitelist - filteredContacts.map { it.number }.toSet()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.lbl_select_all, filteredContacts.size), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    val dialogListState2 = rememberLazyListState()
                    LazyColumn(
                        state = dialogListState2,
                        modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).simpleVerticalScrollbar(dialogListState2).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredContacts) { match ->
                            val isChecked = selectedContactsForWhitelist.contains(match.number)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedContactsForWhitelist = if (isChecked) {
                                            selectedContactsForWhitelist - match.number
                                        } else {
                                            selectedContactsForWhitelist + match.number
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedContactsForWhitelist = if (checked) {
                                            selectedContactsForWhitelist + match.number
                                        } else {
                                            selectedContactsForWhitelist - match.number
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(match.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(match.number, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val toAdd = allContacts.filter { selectedContactsForWhitelist.contains(it.number) }
                    toAdd.forEach { 
                        viewModel.addWhitelist(com.ezirs.data.WhitelistNumber(name = it.name, number = it.number))
                    }
                    showContactDialog = false
                    selectedContactsForWhitelist = emptySet()
                }) {
                    Text(stringResource(R.string.btn_add_contacts, selectedContactsForWhitelist.size))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showContactDialog = false 
                    selectedContactsForWhitelist = emptySet()
                }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.title_add_manual)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.lbl_name_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNumber,
                        onValueChange = { newNumber = it },
                        label = { Text(stringResource(R.string.lbl_phone_number)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newNumber.isNotBlank()) {
                        viewModel.addWhitelist(com.ezirs.data.WhitelistNumber(name = newName.ifBlank { context.getString(R.string.lbl_anonymous) }, number = newNumber))
                        newName = ""
                        newNumber = ""
                        showAddDialog = false
                    }
                }) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.title_whitelist_list), style = MaterialTheme.typography.titleLarge)
            Box {
                Button(onClick = { menuExpanded = true }) {
                    Text(stringResource(R.string.btn_add))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.lbl_manual)) },
                        onClick = {
                            menuExpanded = false
                            showAddDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.lbl_from_contacts)) },
                        onClick = {
                            menuExpanded = false
                            showContactDialog = true
                        }
                    )
                }
            }
        }

        if (whitelists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.msg_no_whitelist), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val logsListState = rememberLazyListState()
        LazyColumn(
            state = logsListState,
            modifier = Modifier.simpleVerticalScrollbar(logsListState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                items(whitelists) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(item.number, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                            }
                            IconButton(onClick = { viewModel.deleteWhitelist(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ContactMatch(val name: String, val number: String)

fun checkContactsMatch(context: android.content.Context, ruleType: com.ezirs.data.RuleType, ruleValue: String, whitelists: List<com.ezirs.data.WhitelistNumber>): List<ContactMatch> {
    val contacts = getAllContacts(context)
    return contacts.filter { contact ->
        val cleanNum = contact.number.replace(" ", "").replace("-", "")
        if (whitelists.any { w -> w.number.replace(" ", "").replace("-", "") == cleanNum }) {
            false
        } else {
            if (ruleType == com.ezirs.data.RuleType.TEPAT) {
                cleanNum == ruleValue
            } else if (ruleType == com.ezirs.data.RuleType.AWALAN) {
                cleanNum.startsWith(ruleValue)
            } else if (ruleType == com.ezirs.data.RuleType.AKHIRAN) {
                cleanNum.endsWith(ruleValue)
            } else {
                cleanNum.contains(ruleValue)
            }
        }
    }
}

fun getAllContacts(context: android.content.Context): List<ContactMatch> {
    val contacts = mutableListOf<ContactMatch>()
    val cursor = context.contentResolver.query(
        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    )
    cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val name = it.getString(nameIndex) ?: ""
            val number = it.getString(numberIndex) ?: ""
            contacts.add(ContactMatch(name, number))
        }
    }
    return contacts
}
