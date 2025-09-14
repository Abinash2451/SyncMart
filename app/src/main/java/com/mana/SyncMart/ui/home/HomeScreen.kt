package com.mana.SyncMart.ui.home

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateToProfile: () -> Unit = {},
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

    // Tab state for switching between sections
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Share dialog states
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedListForShare by remember { mutableStateOf<Pair<String, String>?>(null) } // (id, name)
    var selectedFriendForShare by remember { mutableStateOf<User?>(null) }
    var selectedListForTextShare by remember { mutableStateOf<Pair<String, String>?>(null) } // (id, name) for text sharing

    // Get current user ID
    val currentUserId = authViewModel.getCurrentUserId()
    val context = LocalContext.current

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

    // Separate lists into owned and shared
    val ownedLists = shoppingLists.filter { it.ownerId == currentUserId }
    val sharedLists = shoppingLists.filter { it.ownerId != currentUserId }

    // Tab titles
    val tabTitles = listOf("My Lists", "Shared with Me")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Shopping Lists") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
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
                    enabled = ownedLists.isNotEmpty() && friends.isNotEmpty(),
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
            if (ownedLists.isNotEmpty() && friends.isEmpty() && !friendsLoading) {
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

            // Tab navigation
            if (shoppingLists.isNotEmpty() || !isLoading) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = when (index) {
                                        0 -> "$title (${ownedLists.size})"
                                        1 -> "$title (${sharedLists.size})"
                                        else -> title
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // List content based on selected tab
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
                when (selectedTabIndex) {
                    0 -> {
                        // My Lists tab
                        if (ownedLists.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "No lists created yet",
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
                                items(ownedLists) { list ->
                                    ListCard(
                                        list = list,
                                        currentUserId = currentUserId,
                                        friends = friends,
                                        onListClick = { onNavigateToListDetail(list.id) },
                                        onEditClick = {
                                            editingListId = list.id
                                            editingListName = list.name
                                            showEditDialog = true
                                        },
                                        onDeleteClick = {
                                            deletingListId = list.id
                                            deletingListName = list.name
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Shared with Me tab
                        if (sharedLists.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No shared lists yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        "Lists shared by friends will appear here",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            LazyColumn {
                                items(sharedLists) { list ->
                                    val ownerName = friends.find { it.uid == list.ownerId }?.name ?: "Friend"
                                    ListCard(
                                        list = list,
                                        currentUserId = currentUserId,
                                        friends = friends,
                                        ownerName = ownerName,
                                        onListClick = { onNavigateToListDetail(list.id) }
                                    )
                                }
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

    // Enhanced Share Lists Dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = {
                showShareDialog = false
                selectedListForShare = null
                selectedFriendForShare = null
                selectedListForTextShare = null
            },
            title = { Text("Share Shopping List") },
            text = {
                Column {
                    // Step 1: Select List
                    Text(
                        "Select a list to share:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

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
                                        selectedListForTextShare = list.id to list.name
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

                    // Step 2: Choose sharing method
                    selectedListForShare?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Choose sharing method:",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Share with friend option (bigger section)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Share with friends in app",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

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
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.height(120.dp)
                                    ) {
                                        items(friends) { friend ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 1.dp)
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
                                                            modifier = Modifier.size(16.dp)
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Share as text option (smaller section)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    shareListAsTextWithItems(context, selectedListForTextShare!!.first, selectedListForTextShare!!.second, shoppingListViewModel)
                                    showShareDialog = false
                                    selectedListForShare = null
                                    selectedFriendForShare = null
                                    selectedListForTextShare = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Share as text to other apps",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        "WhatsApp, Messages, Email, etc.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
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
                                selectedListForTextShare = null
                            }
                        }
                    },
                    enabled = selectedListForShare != null && selectedFriendForShare != null
                ) {
                    Text("Share with Friend")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showShareDialog = false
                        selectedListForShare = null
                        selectedFriendForShare = null
                        selectedListForTextShare = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ListCard(
    list: com.mana.SyncMart.data.model.ShoppingList,
    currentUserId: String?,
    friends: List<User>,
    ownerName: String? = null,
    onListClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onListClick() },
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
                        list.ownerId != currentUserId -> "Shared by ${ownerName ?: "Friend"}"
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
                        onEditClick?.let {
                            IconButton(onClick = it) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit List",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Delete button
                        onDeleteClick?.let {
                            IconButton(onClick = it) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete List",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
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

// Enhanced function to share list with actual items from the list
private fun shareListAsTextWithItems(
    context: Context,
    listId: String,
    listName: String,
    shoppingListViewModel: ShoppingListViewModel
) {
    // Use Firestore to get items with a one-time fetch (not a listener)
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    db.collection("shopping_lists")
        .document(listId)
        .collection("items")
        .get()
        .addOnSuccessListener { querySnapshot ->
            val items = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data
                    val quantity = when (val qty = data?.get("quantity")) {
                        is String -> qty
                        is Number -> qty.toString()
                        else -> "1"
                    }

                    com.mana.SyncMart.data.model.Item(
                        id = document.id,
                        name = data?.get("name") as? String ?: "",
                        quantity = quantity,
                        purchased = data?.get("purchased") as? Boolean ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val shareText = buildString {
                appendLine(listName)
                appendLine()

                if (items.isEmpty()) {
                    appendLine("No items in this list")
                } else {
                    items.forEach { item ->
                        val status = if (item.purchased) "✅" else "⬜"
                        appendLine("$status ${item.name} - Qty: ${item.quantity}")
                    }
                }

                appendLine()
                appendLine("Shared from SyncMart")
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, "Share shopping list via")
            context.startActivity(shareIntent)
        }
        .addOnFailureListener { error ->
            // Fallback to basic sharing if items can't be loaded
            val shareText = buildString {
                appendLine(listName)
                appendLine()
                appendLine("Unable to load items. Please open the SyncMart app to view the complete list!")
                appendLine()
                appendLine("Shared from SyncMart")
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, "Share shopping list via")
            context.startActivity(shareIntent)
        }
}

// Alternative implementation that takes items as parameter (for use when items are already loaded)
private fun shareListAsTextWithLoadedItems(
    context: Context,
    listName: String,
    items: List<com.mana.SyncMart.data.model.Item>
) {
    val shareText = buildString {
        appendLine("📝 $listName")
        appendLine()

        if (items.isEmpty()) {
            appendLine("No items in this list")
        } else {
            items.forEachIndexed { index, item ->
                val status = if (item.purchased) "✅" else "⬜"
                appendLine("${index + 1}. $status ${item.name} - Qty: ${item.quantity}")
            }
        }

        appendLine()
        appendLine("Shared from SyncMart")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share shopping list via")
    context.startActivity(shareIntent)
}