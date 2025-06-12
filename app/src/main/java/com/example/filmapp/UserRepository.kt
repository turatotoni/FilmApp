package com.example.filmapp

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun searchUsers(query: String): List<User> {
        return try {
            //dohvacamo kolekciju usera iz baze
            val snapshot = usersCollection
                .orderBy("username")
                .startAt(query)//gledamo po upitu
                .endAt("$query\uf8ff")
                .limit(10)//max 10 usera
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                document.toUser() //pretvaramo dokument u usreobjekt
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun DocumentSnapshot.toUser(): User? {
        return try { //vracamo user objekt
            User(
                id = id,
                username = getString("username") ?: "",
                email = getString("email") ?: "",
                avatarID = getLong("avatarID")?.toInt() ?: R.drawable.ic_profile_placeholder,
                followerCount = getLong("followerCount") ?: 0,
                followingCount = getLong("followingCount") ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }
}