package com.faraoncuba.wassapeame

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etCountry = findViewById<EditText>(R.id.etCountry)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        btnSend.setOnClickListener {
            var country = etCountry.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val message = etMessage.text.toString()

            if (country.startsWith("+")) country = country.substring(1)
            val fullNumber = (country + phone).replace(Regex("[^0-9]"), "")

            if (fullNumber.isEmpty()) {
                Toast.makeText(this, "Introduce un número válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            // Intent directo a la app WhatsApp
            val appIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("whatsapp://send?phone=$fullNumber&text=$encodedMsg")
            ).apply {
                setPackage("com.whatsapp")
            }

            try {
                startActivity(appIntent)
            } catch (e: ActivityNotFoundException) {
                // fallback: abrir en navegador usando la API web de WhatsApp
                val webUrl = "https://api.whatsapp.com/send?phone=$fullNumber&text=$encodedMsg"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}