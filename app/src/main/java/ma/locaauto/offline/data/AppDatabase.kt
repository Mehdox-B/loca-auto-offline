package ma.locaauto.offline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Car::class, Client::class, Reservation::class, Contract::class, Invoice::class, MaintenanceRecord::class, Expense::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rentalDao(): RentalDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context, scope: CoroutineScope): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "locaauto_offline.db")
                .addCallback(SeedCallback(scope))
                .build()
                .also { instance = it }
        }
    }

    private class SeedCallback(private val scope: CoroutineScope) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            instance?.let { database -> scope.launch(Dispatchers.IO) { seed(database.rentalDao()) } }
        }

        private suspend fun seed(dao: RentalDao) {
            val now = System.currentTimeMillis()
            val day = 86_400_000L
            val cars = listOf(
                Car(brand = "Dacia", model = "Logan", category = "Économique", transmission = "Manuelle", fuelType = "Essence", dailyRate = 220.0, licensePlate = "12345-A-6", mileage = 42_100, year = 2023),
                Car(brand = "Dacia", model = "Duster", category = "SUV", transmission = "Manuelle", fuelType = "Diesel", dailyRate = 320.0, licensePlate = "23456-B-7", mileage = 31_500, year = 2024),
                Car(brand = "Renault", model = "Clio", category = "Citadine", transmission = "Automatique", fuelType = "Essence", dailyRate = 280.0, licensePlate = "34567-C-8", mileage = 18_200, year = 2024),
                Car(brand = "Peugeot", model = "3008", category = "SUV", transmission = "Automatique", fuelType = "Hybride", dailyRate = 520.0, licensePlate = "45678-D-9", mileage = 12_800, year = 2025),
                Car(brand = "Mercedes-Benz", model = "Classe C", category = "Premium", transmission = "Automatique", fuelType = "Hybride", dailyRate = 850.0, licensePlate = "56789-E-1", mileage = 8_400, year = 2025, status = CarStatus.MAINTENANCE)
            )
            cars.forEach { dao.insertCar(it) }

            val clients = listOf(
                Client(fullName = "Youssef El Mansouri", phone = "+212 6 12 34 56 78", email = "youssef@example.ma", driverLicenseNumber = "MA-452198", identityNumber = "AB123456", address = "Casablanca"),
                Client(fullName = "Salma Benjelloun", phone = "+212 6 98 76 54 32", email = "salma@example.ma", driverLicenseNumber = "MA-784512", identityNumber = "CD789012", address = "Rabat"),
                Client(fullName = "Omar Alaoui", phone = "+212 6 44 55 66 77", email = "omar@example.ma", driverLicenseNumber = "MA-225588", identityNumber = "EF345678", address = "Marrakech")
            )
            clients.forEach { dao.insertClient(it) }

            val firstReservation = Reservation(carId = 2, clientId = 1, startDate = now - day, endDate = now + 3 * day, dailyRate = 320.0, totalDays = 4, optionsCost = 120.0, options = "Assurance tous risques", totalPrice = 1_400.0, status = RentalStatus.ACTIVE, createdAt = now - 2 * day)
            val firstId = dao.insertReservation(firstReservation).toInt()
            dao.updateCar(cars[1].copy(id = 2, status = CarStatus.RENTED))
            val contractId = dao.insertContract(Contract(reservationId = firstId, number = "CTR-2025-0001", startMileage = 31_500, depositAmount = 1_500.0, status = ContractStatus.ACTIVE)).toInt()
            dao.insertInvoice(Invoice(reservationId = firstId, contractId = contractId, number = "FAC-2025-0001", subtotal = 1_166.67, tax = 233.33, total = 1_400.0, paymentStatus = PaymentStatus.PAID, paymentMethod = "Carte bancaire"))

            dao.insertReservation(Reservation(carId = 1, clientId = 2, startDate = now + 2 * day, endDate = now + 5 * day, dailyRate = 220.0, totalDays = 3, totalPrice = 660.0, status = RentalStatus.CONFIRMED))
            dao.insertReservation(Reservation(carId = 3, clientId = 3, startDate = now - 12 * day, endDate = now - 8 * day, dailyRate = 280.0, totalDays = 4, totalPrice = 1_120.0, status = RentalStatus.COMPLETED))

            dao.insertMaintenance(MaintenanceRecord(carId = 5, description = "Révision périodique et vidange", cost = 1_250.0, status = "En cours"))
            dao.insertExpense(Expense(category = "Assurance", description = "Assurance flotte - trimestre", amount = 4_800.0))
            dao.insertExpense(Expense(category = "Carburant", description = "Carte carburant agence", amount = 1_350.0))
        }
    }
}
