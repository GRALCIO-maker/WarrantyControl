package pl.milosz.warrantycontrol.data

import net.datafaker.Faker
import pl.milosz.warrantycontrol.model.Device
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object FakeDeviceProvider {

    private val faker = Faker(Locale("pl"))

    fun createDevices(): List<Device> {
        val today = LocalDate.now()

        return listOf(
            Device(
                id = 1,
                name = "Laptop Lenovo",
                store = "Media Expert",
                purchaseDate = today.minusMonths(11).format(DateTimeFormatter.ISO_LOCAL_DATE),
                warrantyMonths = 24
            ),
            Device(
                id = 2,
                name = "Słuchawki Sony",
                store = "RTV Euro AGD",
                purchaseDate = today.minusMonths(23).format(DateTimeFormatter.ISO_LOCAL_DATE),
                warrantyMonths = 24
            ),
            Device(
                id = 3,
                name = "${faker.commerce().productName()}",
                store = faker.company().name(),
                purchaseDate = today.minusMonths(30).format(DateTimeFormatter.ISO_LOCAL_DATE),
                warrantyMonths = 24
            )
        )
    }
}
