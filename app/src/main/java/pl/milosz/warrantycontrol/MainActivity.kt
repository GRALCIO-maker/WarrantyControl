package pl.milosz.warrantycontrol

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pl.milosz.warrantycontrol.data.FakeDeviceProvider
import pl.milosz.warrantycontrol.model.Device
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private var reminderDays = 14
    private val devices =
        mutableListOf<pl.milosz.warrantycontrol.model.Device>()
    private lateinit var homeScreen: View
    private lateinit var activeScreen: LinearLayout
    private lateinit var archiveScreen: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderDays = getSharedPreferences(
            "WarrantyControlSettings",
            MODE_PRIVATE
        ).getInt("reminderDays", 14)
        setContentView(R.layout.activity_main)

        homeScreen = findViewById(R.id.homeScreen)
        activeScreen = findViewById(R.id.activeScreen)
        archiveScreen = findViewById(R.id.archiveScreen)

        devices.clear()
        devices.addAll(FakeDeviceProvider.createDevices())
        refreshDevices()

        findViewById<Button>(R.id.btnHome).setOnClickListener {
            showScreen(homeScreen)
        }

        findViewById<Button>(R.id.btnActive).setOnClickListener {
            showScreen(activeScreen)
        }

        findViewById<Button>(R.id.btnArchive).setOnClickListener {
            showScreen(archiveScreen)
        }
        findViewById<Button>(R.id.btnAddDevice).setOnClickListener {
            showAddDeviceDialog()
        }
        findViewById<Button>(R.id.btnReminderSettings).setOnClickListener {
            showReminderSettings()
        }

        showScreen(homeScreen)
    }

    private fun showScreen(screenToShow: View) {
        homeScreen.visibility = if (screenToShow == homeScreen) View.VISIBLE else View.GONE
        activeScreen.visibility = if (screenToShow == activeScreen) View.VISIBLE else View.GONE
        archiveScreen.visibility = if (screenToShow == archiveScreen) View.VISIBLE else View.GONE
    }

    private fun createDeviceCard(device: Device): LinearLayout {
        val daysLeft = device.daysUntilWarrantyEnds()
        val expired = !device.isWarrantyActive()
        val expiringSoon = !expired && daysLeft < 30

        val borderColor = when {
            expired -> Color.rgb(182, 79, 85)
            expiringSoon -> Color.rgb(230, 160, 75)
            else -> Color.rgb(232, 229, 237)
        }

        val statusColor = when {
            expired -> Color.rgb(182, 79, 85)
            expiringSoon -> Color.rgb(166, 106, 39)
            else -> Color.rgb(50, 118, 91)
        }

        val cardBackground = when {
            expired -> Color.rgb(255, 245, 245)
            expiringSoon -> Color.rgb(255, 250, 240)
            else -> Color.WHITE
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(cardBackground)
                cornerRadius = 24f
                setStroke(2, borderColor)
            }

            elevation = 3f

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        card.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(device.name)
                .setItems(arrayOf("Edytuj", "Usuń")) { _, which ->
                    when (which) {
                        0 -> showEditDeviceDialog(device)
                        1 -> confirmDeleteDevice(device)
                    }
                }
                .show()
        }

        card.addView(TextView(this).apply {
            text = device.name
            textSize = 18f
            setTextColor(Color.rgb(36, 34, 53))
            setTypeface(null, Typeface.BOLD)
        })

        card.addView(TextView(this).apply {
            text = "Sklep: ${device.store}"
            textSize = 14f
            setTextColor(Color.rgb(119, 116, 134))
            setPadding(0, 8, 0, 0)
        })

        card.addView(TextView(this).apply {
            text = "Koniec gwarancji: ${
                device.warrantyEndDate().format(
                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
                )
            }"
            textSize = 14f
            setTextColor(Color.rgb(119, 116, 134))
            setPadding(0, 6, 0, 0)
        })

        card.addView(TextView(this).apply {
            text = when {
                expired -> "Gwarancja wygasła"
                expiringSoon -> "Uwaga! Gwarancja kończy się za $daysLeft dni"
                else -> "Gwarancja aktywna • $daysLeft dni"
            }
            textSize = 14f
            setPadding(0, 10, 0, 0)
            setTextColor(statusColor)
            setTypeface(null, Typeface.BOLD)
        })

        return card
    }
    private fun showAddDeviceDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }

        fun addInput(hintText: String): android.widget.EditText {
            val input = android.widget.EditText(this).apply {
                hint = hintText
                setSingleLine(true)
            }
            form.addView(input)
            return input
        }

        val nameInput = addInput("Nazwa urządzenia")
        val storeInput = addInput("Sklep")
        val dateInput = addInput("Data zakupu (RRRR-MM-DD)")
        val monthsInput = addInput("Gwarancja w miesiącach")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Dodaj urządzenie")
            .setView(form)
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("Dodaj", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {
                            val name = nameInput.text.toString().trim()
                            val store = storeInput.text.toString().trim()
                            val date = dateInput.text.toString().trim()
                            val months = monthsInput.text.toString().toIntOrNull()

                            if (name.isBlank() || store.isBlank() ||
                                months == null || months <= 0
                            ) {
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    "Uzupełnij nazwę, sklep i poprawny okres gwarancji",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                return@setOnClickListener
                            }

                            val purchaseDate = try {
                                java.time.LocalDate.parse(date)
                            } catch (_: Exception) {
                                null
                            }

                            if (purchaseDate == null || purchaseDate.isAfter(java.time.LocalDate.now())) {
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    "Podaj poprawną datę zakupu w formacie RRRR-MM-DD",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                return@setOnClickListener
                            }

                            val newDevice = Device(
                                id = (devices.maxOfOrNull { it.id } ?: 0) + 1,
                                name = name,
                                store = store,
                                purchaseDate = purchaseDate.toString(),
                                warrantyMonths = months
                            )

                            devices.add(newDevice)
                            refreshDevices()

                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Dodano urządzenie: $name",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()

                            dismiss()
                        }
                }
                show()
            }
    }
    private fun refreshDevices() {
        val activeDevices = devices.filter { it.isWarrantyActive() }
        val archivedDevices = devices.filter { !it.isWarrantyActive() }

        val expiringSoon = activeDevices.count {
            it.daysUntilWarrantyEnds() in 0..30
        }

        findViewById<TextView>(R.id.tvSummary).text =
            "Aktywne gwarancje: ${activeDevices.size}\n\n" +
                    "Kończące się w ciągu 30 dni: $expiringSoon\n\n" +
                    "Wygasłe gwarancje: ${archivedDevices.size}"
        findViewById<TextView>(R.id.tvActiveCount).text = activeDevices.size.toString()

        val activeList = findViewById<LinearLayout>(R.id.activeDeviceList)
        val archiveList = findViewById<LinearLayout>(R.id.archiveDeviceList)

        activeList.removeAllViews()
        archiveList.removeAllViews()

        activeDevices.forEach { activeList.addView(createDeviceCard(it)) }
        archivedDevices.forEach { archiveList.addView(createDeviceCard(it)) }
    }
    private fun showEditDeviceDialog(device: Device) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }

        fun addInput(hintText: String, initialValue: String): android.widget.EditText {
            val input = android.widget.EditText(this).apply {
                hint = hintText
                setSingleLine(true)
                setText(initialValue)
            }
            form.addView(input)
            return input
        }

        val nameInput = addInput("Nazwa urządzenia", device.name)
        val storeInput = addInput("Sklep", device.store)
        val dateInput = addInput("Data zakupu (RRRR-MM-DD)", device.purchaseDate)
        val monthsInput = addInput("Gwarancja w miesiącach", device.warrantyMonths.toString())

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edytuj urządzenie")
            .setView(form)
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("Zapisz", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {
                            val name = nameInput.text.toString().trim()
                            val store = storeInput.text.toString().trim()
                            val date = dateInput.text.toString().trim()
                            val months = monthsInput.text.toString().toIntOrNull()

                            if (name.isBlank() || store.isBlank() ||
                                months == null || months <= 0
                            ) {
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    "Uzupełnij nazwę, sklep i poprawny okres gwarancji",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                return@setOnClickListener
                            }

                            val purchaseDate = try {
                                java.time.LocalDate.parse(date)
                            } catch (_: Exception) {
                                null
                            }

                            if (purchaseDate == null ||
                                purchaseDate.isAfter(java.time.LocalDate.now())
                            ) {
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    "Podaj poprawną datę w formacie RRRR-MM-DD",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                return@setOnClickListener
                            }

                            val index = devices.indexOfFirst { it.id == device.id }

                            if (index != -1) {
                                devices[index] = Device(
                                    id = device.id,
                                    name = name,
                                    store = store,
                                    purchaseDate = purchaseDate.toString(),
                                    warrantyMonths = months
                                )
                            }

                            refreshDevices()

                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Zapisano zmiany",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()

                            dismiss()
                        }
                }
                show()
            }
    }
    private fun confirmDeleteDevice(device: Device) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Usunąć urządzenie?")
            .setMessage("Czy na pewno chcesz usunąć „${device.name}”?")
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("Usuń") { _, _ ->
                devices.removeAll { it.id == device.id }
                refreshDevices()

                android.widget.Toast.makeText(
                    this,
                    "Usunięto urządzenie",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }
    private fun showReminderSettings() {
        val options = arrayOf(
            "1 dzień przed końcem",
            "3 dni przed końcem",
            "7 dni przed końcem",
            "14 dni przed końcem",
            "30 dni przed końcem"
        )

        val values = arrayOf(1, 3, 7, 14, 30)
        val selectedIndex = values.indexOf(reminderDays).coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Przypomnienie o gwarancji")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                reminderDays = values[which]
                getSharedPreferences(
                    "WarrantyControlSettings",
                    MODE_PRIVATE
                ).edit()
                    .putInt("reminderDays", reminderDays)
                    .apply()

                android.widget.Toast.makeText(
                    this,
                    "Ustawiono przypomnienie: $reminderDays dni przed końcem gwarancji",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
}
