package com.faraoncuba.wassapeame

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinnerCountry = findViewById<Spinner>(R.id.spinnerCountry)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        // Cargar arrays desde resources
        val countryNames = resources.getStringArray(R.array.country_names)
        val countryCodes = resources.getStringArray(R.array.country_codes)

        // Adapter para el spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCountry.adapter = adapter

        // Mapa ISO -> prefijo (ejemplos representativos; la lista se puede ampliar)
        val isoToCode = mapOf(
            "af" to "93","al" to "355","dz" to "213","ad" to "376","ao" to "244",
            "ar" to "54","am" to "374","au" to "61","at" to "43","az" to "994",
            "bd" to "880","be" to "32","bo" to "591","br" to "55","bg" to "359",
            "ca" to "1","cl" to "56","cn" to "86","co" to "57","cr" to "506",
            "cu" to "53","cz" to "420","dk" to "45","do" to "1","ec" to "593",
            "eg" to "20","sv" to "503","ee" to "372","fi" to "358","fr" to "33",
            "de" to "49","gh" to "233","gr" to "30","gt" to "502","hn" to "504",
            "hk" to "852","hu" to "36","in" to "91","id" to "62","ie" to "353",
            "il" to "972","it" to "39","jp" to "81","kz" to "7","ke" to "254",
            "kr" to "82","kw" to "965","lb" to "961","lt" to "370","lu" to "352",
            "my" to "60","mx" to "52","ma" to "212","nl" to "31","nz" to "64",
            "ng" to "234","no" to "47","pk" to "92","pe" to "51","ph" to "63",
            "pl" to "48","pt" to "351","pr" to "1","ro" to "40","ru" to "7",
            "sa" to "966","rs" to "381","sg" to "65","sk" to "421","si" to "386",
            "za" to "27","es" to "34","se" to "46","ch" to "41","tw" to "886",
            "th" to "66","tr" to "90","ua" to "380","ae" to "971","gb" to "44",
            "us" to "1","uy" to "598","ve" to "58"
        )

        // Intent: intentar detectar país operador/SIM y preseleccionar en spinner
        try {
            val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            var iso: String? = tm.simCountryIso
            if (iso.isNullOrBlank()) iso = tm.networkCountryIso
            if (iso.isNullOrBlank()) {
                // Locale.getDefault().country suele devolver codes en mayúsculas (ej. "ES")
                val localeCountry = Locale.getDefault().country
                iso = if (localeCountry.isNullOrBlank()) null else localeCountry.lowercase(Locale.ROOT)
            } else {
                iso = iso.lowercase(Locale.ROOT)
            }

            if (!iso.isNullOrBlank()) {
                val prefix = isoToCode[iso]
                var index = -1
                if (!prefix.isNullOrBlank()) {
                    // Para prefijos compartidos (ej. NANP +1) intentamos buscar por nombre si es necesario
                    if (prefix == "1") {
                        // intentar buscar Estados Unidos o Canada en countryNames
                        index = countryNames.indexOfFirst {
                            it.contains("United States", ignoreCase = true) ||
                            it.contains("United States of America", ignoreCase = true) ||
                            it.contains("Canada", ignoreCase = true)
                        }
                    }
                    if (index < 0) {
                        // buscar por código exacto en country_codes
                        index = countryCodes.indexOf(prefix)
                        // si no encuentra código exacto, buscar códigos que contengan el prefijo (p.ej. "1242")
                        if (index < 0) {
                            index = countryCodes.indexOfFirst { it == prefix || it.endsWith(prefix) || it.startsWith(prefix) }
                        }
                    }
                }

                if (index >= 0) {
                    spinnerCountry.setSelection(index)
                } else {
                    // fallback: buscar por nombre local del país
                    val localeCountryName = Locale("", iso.uppercase(Locale.ROOT)).displayCountry
                    val idxByName = countryNames.indexOfFirst { it.contains(localeCountryName, ignoreCase = true) }
                    if (idxByName >= 0) spinnerCountry.setSelection(idxByName)
                }
            }
        } catch (t: Throwable) {
            // No bloquear la UI si falla la detección; simplemente no preseleccionamos
        }

        btnSend.setOnClickListener {
            val selectedPos = spinnerCountry.selectedItemPosition
            var country = if (selectedPos in countryCodes.indices) countryCodes[selectedPos] else ""
            val phone = etPhone.text.toString().trim()
            val message = etMessage.text.toString()

            if (country.startsWith("+")) country = country.substring(1)
            val fullNumber = (country + phone).replace(Regex("[^0-9]"), "")

            if (fullNumber.isEmpty()) {
                Toast.makeText(this, "Introduce un número válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            // Intent directo a la app WhatsApp; intentar WhatsApp normal, luego WhatsApp Business, luego web
            val appIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("whatsapp://send?phone=$fullNumber&text=$encodedMsg")
            ).apply { setPackage("com.whatsapp") }

            try {
                startActivity(appIntent)
            } catch (e: ActivityNotFoundException) {
                // Intent para WhatsApp Business
                try {
                    val businessIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("whatsapp://send?phone=$fullNumber&text=$encodedMsg")
                    ).apply { setPackage("com.whatsapp.w4b") }
                    startActivity(businessIntent)
                } catch (e2: ActivityNotFoundException) {
                    // fallback: abrir en navegador usando la API web de WhatsApp
                    val webUrl = "https://api.whatsapp.com/send?phone=$fullNumber&text=$encodedMsg"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                } catch (e2: Exception) {
                    Toast.makeText(this, "Error: ${e2.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
