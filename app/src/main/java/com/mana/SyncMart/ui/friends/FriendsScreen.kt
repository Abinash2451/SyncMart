package com.mana.SyncMart.ui.friends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mana.SyncMart.viewmodel.FriendsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onSharedListClick: () -> Unit,
    onBack: () -> Unit,
    friendsViewModel: FriendsViewModel = viewModel()
) {
    val friends by friendsViewModel.friends.collectAsState()
    val isLoading by friendsViewModel.isLoading.collectAsState()
    val errorMessage by friendsViewModel.errorMessage.collectAsState()
    val searchResult by friendsViewModel.searchResult.collectAsState()
    var searchEmail by remember { mutableStateOf("") }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var isAddingFriend by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        friendsViewModel.loadFriends()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddFriendDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Friend")
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
            // Shared Lists Button
            Button(
                onClick = onSharedListClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Shared Lists")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Loading indicator
            if (isLoading && friends.isEmpty()) {
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

            // Friends List
            if (friends.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No friends yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "Add friends to share shopping lists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(friends) { friend ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    // Profile picture placeholder
                                    Card(
                                        modifier = Modifier.size(40.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (friend.displayName.takeIf { it.isNotBlank() }
                                                    ?: friend.name.takeIf { it.isNotBlank() }
                                                    ?: friend.email.substringBefore("@"))
                                                    .first().uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        // Display name (primary)
                                        Text(
                                            text = friend.displayName.takeIf { it.isNotBlank() }
                                                ?: friend.name.takeIf { it.isNotBlank() }
                                                ?: friend.email.substringBefore("@"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )

                                        // Email (secondary)
                                        Text(
                                            text = friend.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )

                                        // Show sign-in method if relevant
                                        if (friend.signInProvider.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    when (friend.signInProvider) {
                                                        "google" -> Icons.Default.Email
                                                        else -> Icons.Default.Person
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.outline
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = when (friend.signInProvider) {
                                                        "google" -> "Google user"
                                                        "email" -> "Email user"
                                                        else -> ""
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { friendsViewModel.removeFriend(friend.uid) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove Friend",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Friend Dialog
    if (showAddFriendDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddFriendDialog = false
                friendsViewModel.clearSearchResult()
                searchEmail = ""
                isAddingFriend = false
            },
            title = { Text("Add Friend") },
            text = {
                Column {
                    Text(
                        text = "Search for friends by their email address",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = searchEmail,
                        onValueChange = { newEmail ->
                            val previousEmail = searchEmail
                            searchEmail = newEmail

                            // Clear search if email is blank
                            if (newEmail.isBlank()) {
                                friendsViewModel.clearSearchResult()
                            }
                            // Search if email has changed and is not blank
                            else if (newEmail.trim() != previousEmail.trim() && newEmail.trim().isNotBlank()) {
                                friendsViewModel.searchUser(newEmail.trim())
                            }
                        },
                        label = { Text("Friend's Email") },
                        trailingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAddingFriend
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Show loading indicator while searching
                    if (isLoading && searchEmail.isNotBlank()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Searching...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Show search result
                    searchResult?.let { user ->
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
                                // Profile picture
                                Card(
                                    modifier = Modifier.size(32.dp),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (user.displayName.takeIf { it.isNotBlank() }
                                                ?: user.name.takeIf { it.isNotBlank() }
                                                ?: user.email.substringBefore("@"))
                                                .first().uppercase(),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Found: ${user.displayName.takeIf { it.isNotBlank() }
                                            ?: user.name.takeIf { it.isNotBlank() }
                                            ?: user.email.substringBefore("@")}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Show message if no user found
                    if (!isLoading && searchEmail.isNotBlank() && searchResult == null) {
                        Text(
                            text = "No user found with this email",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        searchResult?.let { user ->
                            isAddingFriend = true
                            friendsViewModel.addFriend(user.uid)
                            // Close dialog after adding
                            showAddFriendDialog = false
                            searchEmail = ""
                            isAddingFriend = false
                        }
                    },
                    enabled = searchResult != null && !isAddingFriend && !isLoading
                ) {
                    if (isAddingFriend) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adding...")
                        }
                    } else {
                        Text("Add Friend")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddFriendDialog = false
                        friendsViewModel.clearSearchResult()
                        searchEmail = ""
                        isAddingFriend = false
                    },
                    enabled = !isAddingFriend
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}