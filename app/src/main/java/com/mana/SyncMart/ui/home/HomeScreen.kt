package com.mana.SyncMart.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mana.SyncMart.data.model.User
import com.mana.SyncMart.viewmodel.AuthViewModel
import com.mana.SyncMart.viewmodel.ShoppingListViewModel
import com.mana.SyncMart.viewmodel.FriendsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToListDetail: (String) -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onLogout: () -> Unit = {},
    shoppingListViewModel: ShoppingListViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    friendsViewModel: FriendsViewModel = viewModel()
) {
    val shoppingLists by shoppingListViewModel.shoppingLists.collectAsState()
    val isLoading by shoppingListViewModel.isLoading.collectAsState()
    val errorMessage by shoppingListViewModel.errorMessage.collectAsState()
    val successMessage by shoppingListViewModel.successMessage.collectAsState()

    // Friends data
    val friends by friendsViewModel.friends.collectAsState()
    val friendsLoading by friendsViewModel.isLoading.collectAsState()

    var newListName by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingListId by remember { mutableStateOf("") }
    var editingListName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingListId by remember { mutableStateOf("") }
    var deletingListName by remember { mutableStateOf("") }

    // Share dialog states
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedListForShare by remember { mutableStateOf<Pair<String, String>?>(null) } // (id, name)
    var selectedFriendForShare by remember { mutableStateOf<User?>(null) }

    // Get current user ID
    val currentUserId = authViewModel.getCurrentUserId()

    LaunchedEffect(Unit) {
        shoppingListViewModel.fetchUserLists()
        friendsViewModel.loadFriends()
    }

    // Clear success message after showing it
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            shoppingListViewModel.clearSuccess()
        }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            shoppingListViewModel.addShoppingList(newListName)
                            newListName = ""
                        }
                    },
                    enabled = newListName.isNotBlank()
                ) {
                    Text("Add List")
                }

                // Share Lists Button
                Button(
                    onClick = { showShareDialog = true },
                    enabled = shoppingLists.any { it.ownerId == currentUserId } && friends.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Lists")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show success message
            successMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Show message if no friends for sharing
            if (shoppingLists.any { it.ownerId == currentUserId } && friends.isEmpty() && !friendsLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add friends to share your shopping lists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = list.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )

                                            // Show indicator if this is a shared list (not owned by current user)
                                            if (list.ownerId != currentUserId) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = "Shared List",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        val listStatusText = when {
                                            list.ownerId != currentUserId -> "Shared by owner"
                                            list.sharedWith.isNotEmpty() -> "Shared with ${list.sharedWith.size} people"
                                            else -> "Private list"
                                        }

                                        Text(
                                            text = listStatusText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Row {
                                        // Only show edit/delete buttons for lists you own
                                        if (list.ownerId == currentUserId) {
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

    // Share Lists Dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = {
                showShareDialog = false
                selectedListForShare = null
                selectedFriendForShare = null
            },
            title = { Text("Share Shopping List") },
            text = {
                Column {
                    // Step 1: Select List (only show lists owned by current user)
                    Text(
                        "Select a list to share:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val ownedLists = shoppingLists.filter { it.ownerId == currentUserId }

                    LazyColumn(
                        modifier = Modifier.height(120.dp)
                    ) {
                        items(ownedLists) { list ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        selectedListForShare = list.id to list.name
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedListForShare?.first == list.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (selectedListForShare?.first == list.id) 4.dp else 1.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedListForShare?.first == list.id) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = list.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Step 2: Select Friend (only show if list is selected)
                    selectedListForShare?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Select a friend to share with:",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (friendsLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else if (friends.isEmpty()) {
                            Text(
                                "No friends available. Add friends first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(120.dp)
                            ) {
                                items(friends) { friend ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clickable {
                                                selectedFriendForShare = friend
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedFriendForShare?.uid == friend.uid)
                                                MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = if (selectedFriendForShare?.uid == friend.uid) 4.dp else 1.dp
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (selectedFriendForShare?.uid == friend.uid) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Column {
                                                Text(
                                                    text = friend.name,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = friend.email,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedListForShare?.let { (listId, _) ->
                            selectedFriendForShare?.let { friend ->
                                shoppingListViewModel.shareList(listId, friend.email)
                                showShareDialog = false
                                selectedListForShare = null
                                selectedFriendForShare = null
                            }
                        }
                    },
                    enabled = selectedListForShare != null && selectedFriendForShare != null
                ) {
                    Text("Share List")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showShareDialog = false
                        selectedListForShare = null
                        selectedFriendForShare = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}