package com.mana.SyncMart.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mana.SyncMart.data.model.Item
import com.mana.SyncMart.viewmodel.ShoppingListViewModel
import com.mana.SyncMart.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    listId: String,
    onBack: () -> Unit,
    shoppingListViewModel: ShoppingListViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val items by shoppingListViewModel.items.collectAsState()
    val shoppingLists by shoppingListViewModel.shoppingLists.collectAsState()
    val isLoading by shoppingListViewModel.isLoading.collectAsState()
    val errorMessage by shoppingListViewModel.errorMessage.collectAsState()
    var newItemName by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("1") }
    var showAddItemDialog by remember { mutableStateOf(false) }

    // Get the current list name and details
    val currentList = shoppingLists.find { it.id == listId }
    val listName = currentList?.name ?: "Shopping List"
    val currentUserId = authViewModel.getCurrentUserId()

    // Fetch items when listId changes
    LaunchedEffect(listId) {
        shoppingListViewModel.fetchItems(listId)
    }

    // Clean up listeners when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            shoppingListViewModel.clearItems()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = listName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Show real-time sync indicator
                        currentList?.let { list ->
                            val statusText = when {
                                list.ownerId != currentUserId -> "Shared list - real-time sync"
                                list.sharedWith.isNotEmpty() -> "Syncing with ${list.sharedWith.size} people"
                                else -> null
                            }
                            statusText?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddItemDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Show sharing info if list is shared
            currentList?.let { list ->
                if (list.sharedWith.isNotEmpty() || list.ownerId != currentUserId) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val mainText = if (list.ownerId != currentUserId) {
                                    "This is a shared list - changes sync in real-time"
                                } else {
                                    "This list is shared with ${list.sharedWith.size} people"
                                }
                                Text(
                                    text = mainText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (list.ownerId != currentUserId) {
                                    Text(
                                        text = "Any changes you make will be visible to all users instantly",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Loading indicator
            if (isLoading && items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Items List
            if (items.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No items in this list",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "Tap the + button to add items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(items) { item ->
                        ItemCard(
                            item = item,
                            onTogglePurchased = {
                                shoppingListViewModel.updateItem(
                                    listId,
                                    item.copy(purchased = !item.purchased)
                                )
                            },
                            onDeleteItem = {
                                shoppingListViewModel.deleteItem(listId, item.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddItemDialog = false
                newItemName = ""
                newItemQuantity = "1"
            },
            title = { Text("Add Item") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemQuantity,
                        onValueChange = { newItemQuantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            val quantity = newItemQuantity.toIntOrNull() ?: 1
                            val newItem = Item(
                                name = newItemName,
                                quantity = quantity,
                                purchased = false
                            )
                            shoppingListViewModel.addItem(listId, newItem)
                            showAddItemDialog = false
                            newItemName = ""
                            newItemQuantity = "1"
                        }
                    },
                    enabled = newItemName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddItemDialog = false
                        newItemName = ""
                        newItemQuantity = "1"
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ItemCard(
    item: Item,
    onTogglePurchased: () -> Unit,
    onDeleteItem: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.purchased)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = item.purchased,
                    onCheckedChange = { onTogglePurchased() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.purchased)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Quantity: ${item.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.purchased)
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(onClick = onDeleteItem) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}