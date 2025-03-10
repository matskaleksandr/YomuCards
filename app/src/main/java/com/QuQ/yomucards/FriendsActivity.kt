package com.QuQ.yomucards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.QuQ.yomucards.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class FriendsActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var friendRequestsRecyclerView: RecyclerView
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var searchResultsAdapter: UserAdapter // Адаптер для результатов поиска
    private val searchResultsList = mutableListOf<UserF>() // Список результатов поиска

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.friends_activity)

        // Инициализация SearchView
        searchView = findViewById(R.id.searchView)

        // Инициализация RecyclerView для результатов поиска
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        friendRequestsRecyclerView = findViewById(R.id.friendRequestsRecyclerView)
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView)
        friendsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsAdapter = UserAdapter(searchResultsList)
        searchResultsRecyclerView.adapter = searchResultsAdapter

        val exitButton = findViewById<ImageButton>(R.id.btnExitFriends)
        exitButton.setOnClickListener{
            finish()
        }

        // Настройка SearchView
        setupSearchView()

        loadFriendRequests()
        loadFriends()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchUsers(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    // Если строка поиска пуста, скрываем RecyclerView с результатами
                    searchResultsRecyclerView.visibility = View.GONE
                } else {
                    // Если строка поиска не пуста, показываем RecyclerView и выполняем поиск
                    searchResultsRecyclerView.visibility = View.VISIBLE
                    searchUsers(newText)
                }
                return true
            }
        })
    }

    private fun searchUsers(query: String) {
        val database = Firebase.database.reference.child("Users")
        database.orderByChild("Username")
            .startAt(query)
            .endAt("$query\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    searchResultsList.clear()
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val currentUserId = currentUser?.uid // ID текущего пользователя

                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: "" // ID пользователя из базы данных

                        // Пропускаем текущего пользователя
                        if (userId == currentUserId) {
                            continue
                        }

                        val username = userSnapshot.child("Username").getValue(String::class.java)
                        val avatarPath = userSnapshot.child("AvatarPath").getValue(String::class.java) ?: ""
                        val friendRequests = userSnapshot.child("friend_requests").children
                        val friends = userSnapshot.child("friends").children
                        var isFriendRequestSent = false
                        var isFriend = false

                        // Проверяем, отправлена ли заявка текущим пользователем
                        if (currentUser != null) {
                            for (request in friendRequests) {
                                if (request.key == currentUserId) {
                                    isFriendRequestSent = true
                                    break
                                }
                            }

                            // Проверяем, является ли текущий пользователь другом
                            for (friend in friends) {
                                if (friend.key == currentUserId) {
                                    isFriend = true
                                    break
                                }
                            }
                        }

                        if (username != null) {
                            val user = UserF(userId, username, avatarPath, isFriendRequestSent, isFriend)
                            searchResultsList.add(user)
                        }
                    }
                    searchResultsAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FriendsActivity, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateFriendRequestsRecyclerView(friendRequests: MutableList<FriendRequest>) {
        val adapter = FriendRequestAdapter(
            friendRequests,
            onAccept = { acceptFriendRequest(it) },
            onDecline = { declineFriendRequest(it) }
        )
        friendRequestsRecyclerView.adapter = adapter
        friendRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun acceptFriendRequest(friendRequest: FriendRequest) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        val database = Firebase.database.reference

        // Добавляем пользователя в список друзей
        val currentUserFriendsPath = "Users/${currentUser.uid}/friends/${friendRequest.userId}"
        val senderFriendsPath = "Users/${friendRequest.userId}/friends/${currentUser.uid}"

        database.child(currentUserFriendsPath).setValue(true)
        database.child(senderFriendsPath).setValue(true)

        // Удаляем заявку
        val friendRequestPath = "Users/${currentUser.uid}/friend_requests/${friendRequest.userId}"
        database.child(friendRequestPath).removeValue()

        loadFriends()

        Toast.makeText(this, "Заявка принята", Toast.LENGTH_SHORT).show()
    }

    private fun declineFriendRequest(friendRequest: FriendRequest) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        val database = Firebase.database.reference

        // Удаляем заявку
        val friendRequestPath = "Users/${currentUser.uid}/friend_requests/${friendRequest.userId}"
        database.child(friendRequestPath).removeValue()

        Toast.makeText(this, "Заявка отклонена", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        loadFriends()
    }

    private fun loadFriendRequests() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        val database = Firebase.database.reference
        val friendRequestsPath = "Users/${currentUser.uid}/friend_requests"

        database.child(friendRequestsPath).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendRequests = mutableListOf<FriendRequest>()

                for (requestSnapshot in snapshot.children) {
                    val senderId = requestSnapshot.key ?: continue

                    // Загружаем информацию о пользователе, отправившем заявку
                    database.child("Users/$senderId").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val username = userSnapshot.child("Username").getValue(String::class.java)
                            val avatarPath = userSnapshot.child("AvatarPath").getValue(String::class.java)

                            if (username != null && avatarPath != null) {
                                val friendRequest = FriendRequest(senderId, username, avatarPath)
                                friendRequests.add(friendRequest)
                            }

                            // Обновляем RecyclerView после загрузки всех заявок
                            if (friendRequests.size == snapshot.children.count()) {
                                updateFriendRequestsRecyclerView(friendRequests)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@FriendsActivity, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FriendsActivity, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun loadFriends() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        val database = Firebase.database.reference
        val friendsPath = "Users/${currentUser.uid}/friends"

        database.child(friendsPath).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friends = mutableListOf<Friend>()

                for (friendSnapshot in snapshot.children) {
                    val friendId = friendSnapshot.key ?: continue

                    // Загружаем информацию о друге
                    database.child("Users/$friendId").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val username = userSnapshot.child("Username").getValue(String::class.java)
                            val avatarPath = userSnapshot.child("AvatarPath").getValue(String::class.java)

                            if (username != null && avatarPath != null) {
                                val friend = Friend(friendId, username, avatarPath)
                                friends.add(friend)
                            }

                            // Обновляем RecyclerView после загрузки всех друзей
                            if (friends.size == snapshot.children.count()) {
                                updateFriendsRecyclerView(friends)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@FriendsActivity, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FriendsActivity, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateFriendsRecyclerView(friends: MutableList<Friend>) {
        val adapter = FriendsAdapter(
            friends,
            onProfileClick = { friend ->
                val intent = FriendProfileActivity.createIntent(friendsRecyclerView.context, friend)
                friendsRecyclerView.context.startActivity(intent)
            }
        )
        friendsRecyclerView.adapter = adapter
        friendsRecyclerView.layoutManager = LinearLayoutManager(friendsRecyclerView.context)
    }


}

