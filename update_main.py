import re

with open("app/src/main/java/com/ezirs/MainActivity.kt", "r") as f:
    code = f.read()

# 1. Remove NavigationBarItem for Settings
settings_nav_item = """                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.tab_database)) },
                                label = { Text(stringResource(R.string.tab_database)) },
                                selected = currentTab == 4,
                                onClick = { currentTab = 4 }
                            )"""
code = code.replace(settings_nav_item, "")

# 2. Add onOpenSettings to CallBlockerScreen definition and usage
callblocker_usage_old = """                        0 -> CallBlockerScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            newRuleValue = newRuleValue,
                            newRuleType = newRuleType,
                            onValueChange = { newRuleValue = it },
                            onTypeChange = { newRuleType = it },
                            onRequestRole = { requestCallScreeningRole() }
                        )"""
callblocker_usage_new = """                        0 -> CallBlockerScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            newRuleValue = newRuleValue,
                            newRuleType = newRuleType,
                            onValueChange = { newRuleValue = it },
                            onTypeChange = { newRuleType = it },
                            onRequestRole = { requestCallScreeningRole() },
                            onOpenSettings = { currentTab = 4 }
                        )"""
code = code.replace(callblocker_usage_old, callblocker_usage_new)

callblocker_def_old = """fun CallBlockerScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    newRuleValue: String,
    newRuleType: RuleType,
    onValueChange: (String) -> Unit,
    onTypeChange: (RuleType) -> Unit,
    onRequestRole: () -> Unit
) {"""
callblocker_def_new = """fun CallBlockerScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    newRuleValue: String,
    newRuleType: RuleType,
    onValueChange: (String) -> Unit,
    onTypeChange: (RuleType) -> Unit,
    onRequestRole: () -> Unit,
    onOpenSettings: () -> Unit
) {"""
code = code.replace(callblocker_def_old, callblocker_def_new)

# 3. Update Trakteer text/link, and add Settings button next to Active Pill
old_trakteer = """                    Text(
                        text = "Trakteer",
                        style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { uriHandler.openUri("https://trakteer.id/ezirs") }
                    )"""
new_trakteer = """                    Text(
                        text = "Donate",
                        style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { uriHandler.openUri("https://trakteer.id/ezirs/tip") }
                    )"""
code = code.replace(old_trakteer, new_trakteer)

old_active_pill = """            Row(
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
            ) {"""
new_active_pill = """            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                ) {"""
code = code.replace(old_active_pill, new_active_pill)
code = code.replace("            } // Active Pill", "            }\n            }") # fix closing bracket

# 4. Fix Theme Buttons layout in SettingsScreen
old_theme_row = """                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = { 
                            themePref = 0
                            prefs.edit().putInt("theme_pref", 0).apply()
                            (context as? android.app.Activity)?.recreate()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (themePref == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) { Text(stringResource(R.string.theme_system), color = if (themePref == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                    Button(
                        onClick = { 
                            themePref = 1
                            prefs.edit().putInt("theme_pref", 1).apply()
                            (context as? android.app.Activity)?.recreate()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (themePref == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) { Text(stringResource(R.string.theme_light), color = if (themePref == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                    Button(
                        onClick = { 
                            themePref = 2
                            prefs.edit().putInt("theme_pref", 2).apply()
                            (context as? android.app.Activity)?.recreate()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (themePref == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) { Text(stringResource(R.string.theme_dark), color = if (themePref == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                }"""
new_theme_row = """                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }"""
code = code.replace(old_theme_row, new_theme_row)

with open("app/src/main/java/com/ezirs/MainActivity.kt", "w") as f:
    f.write(code)
