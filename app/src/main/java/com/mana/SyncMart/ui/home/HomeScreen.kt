package com.mana.SyncMart.ui.home
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mana.SyncMart.viewmodel.AuthViewModel
import com.mana.SyncMart.viewmodel.ShoppingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToListDetail: (String) -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onLogout: () -> Unit = {},
    shoppingListViewModel: ShoppingListViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val shoppingLists by shoppingListViewModel.shoppingLists.collectAsState()
    val isLoading by shoppingListViewModel.isLoading.collectAsState()
    val errorMessage by shoppingListViewModel.errorMessage.collectAsState()
    var newListName by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingListId by remember { mutableStateOf("") }
    var editingListName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingListId by remember { mutableStateOf("") }
    var deletingListName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        shoppingListViewModel.fetchUserLists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Shopping Lists") },
                actions = {
                    IconButton(onClick = onNavigateToFriends) {
                        Icon(Icons.Default.People, contentDescription = "Friends")
                    }
                    IconButton(
                        onClick = {
                            authViewModel.logout()
                            onLogout()
                        }
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
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
            // Input to add a new list
            OutlinedTextField(
                value = newListName,
                onValueChange = { newListName = it },
                label = { Text("New List Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (newListName.isNotBlank()) {
                        shoppingListViewModel.addShoppingList(newListName)
                        newListName = ""
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = newListName.isNotBlank()
            ) {
                Text("Add List")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Loading indicator
            if (isLoading && shoppingLists.isEmpty()) {
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

            // List of shopping lists
            if (shoppingLists.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No shopping lists yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "Create your first shopping list above",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(shoppingLists) { list ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    onNavigateToListDetail(list.id)
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = list.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = if (list.sharedWith.isNotEmpty())
                                                "Shared with ${list.sharedWith.size} people"
                                            else "Private list",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Row {
                                        // Edit button
                                        IconButton(
                                            onClick = {
                                                editingListId = list.id
                                                editingListName = list.name
                                                showEditDialog = true
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit List",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Delete button
                                        IconButton(
                                            onClick = {
                                                deletingListId = list.id
                                                deletingListName = list.name
                                                showDeleteDialog = true
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete List",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Tap to view →",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit List Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                editingListName = ""
                editingListId = ""
            },
            title = { Text("Edit List Name") },
            text = {
                OutlinedTextField(
                    value = editingListName,
                    onValueChange = { editingListName = it },
                    label = { Text("List Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingListName.isNotBlank()) {
                            shoppingListViewModel.updateShoppingListName(editingListId, editingListName)
                            showEditDialog = false
                            editingListName = ""
                            editingListId = ""
                        }
                    },
                    enabled = editingListName.isNotBlank()
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        editingListName = ""
                        editingListId = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deletingListId = ""
                deletingListName = ""
            },
            title = { Text("Delete List") },
            text = {
                Text("Are you sure you want to delete \"$deletingListName\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        shoppingListViewModel.deleteShoppingList(deletingListId)
                        showDeleteDialog = false
                        deletingListId = ""
                        deletingListName = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deletingListId = ""
                        deletingListName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}