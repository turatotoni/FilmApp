package com.example.filmapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import androidx.recyclerview.widget.GridLayoutManager

//prikaz galerije avatara i mogucnost odabira
class AvatarPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avatar_picker)

        val avatars = listOf(
            R.drawable.avatar1,
            R.drawable.avatar2,
            R.drawable.avatar3,
            R.drawable.avatar4,
            R.drawable.avatar5,
            R.drawable.avatar6,
            R.drawable.avatar7,
            R.drawable.avatar8,
            R.drawable.avatar9
        )

        val recyclerView = findViewById<RecyclerView>(R.id.avatarsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3) //3 stupca
        recyclerView.adapter = AvatarAdapter(avatars) { avatarResId -> //stvaranje nove instance adaptera i proslijeđivanje avatarID
            // Vrati odabrani avatar u ProfileActivity
            val resultIntent = Intent()
            resultIntent.putExtra("avatarResId", avatarResId) //spremanje ID-a
            setResult(RESULT_OK, resultIntent)
            finish()
            //zatvaranje aktivnosti i vracanje na profil
        }
    }
}