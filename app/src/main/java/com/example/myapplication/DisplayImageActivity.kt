package com.example.myapplication

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import java.io.File

class DisplayImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        // Получаем путь к файлу из Intent (если мы его передадим из MainFragment)
        val imagePath = intent.getStringExtra("image_path")

        val imageView = findViewById<ImageView>(R.id.imageFullScreen)

        if (imagePath.isNullOrEmpty()) {
            Toast.makeText(this, "Путь к изображению не передан", Toast.LENGTH_SHORT).show()
            finish() // Закрываем Activity, если нет пути
            return
        }

        val file = File(imagePath)
        if (file.exists()) {
            // Декодируем картинку и устанавливаем
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imageView.setImageBitmap(bitmap)
        } else {
            Toast.makeText(this, "Файл изображения не найден", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
