package ma.locaauto.offline.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object RentalStatus {
    const val PENDING = "En attente"
    const val CONFIRMED = "Confirmée"
    const val ACTIVE = "En cours"
    const val COMPLETED = "Terminée"
    const val CANCELLED = "Annulée"
}

object CarStatus {
    const val AVAILABLE = "Disponible"
    const val RENTED = "Louée"
    const val MAINTENANCE = "Maintenance"
}

object ContractStatus {
    const val PENDING = "En attente"
    const val SIGNED = "Signé"
    const val ACTIVE = "Actif"
    const val CLOSED = "Clôturé"
    const val CANCELLED = "Annulé"
}

object PaymentStatus {
    const val PENDING = "En attente"
    const val PAID = "Payée"
    const val LATE = "En retard"
    const val CANCELLED = "Annulée"
}

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val model: String,
    val category: String,
    val transmission: String,
    val fuelType: String,
    val seats: Int = 5,
    val dailyRate: Double,
    val licensePlate: String,
    val status: String = CarStatus.AVAILABLE,
    val mileage: Int = 0,
    val year: Int = 2025,
    val notes: String = ""
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val phone: String,
    val email: String = "",
    val driverLicenseNumber: String = "",
    val identityNumber: String = "",
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val nationalIdDocumentUri: String = "",
    val driverLicenseDocumentUri: String = ""
)

@Entity(tableName = "reservations")
data class Reservation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val clientId: Int,
    val startDate: Long,
    val endDate: Long,
    val dailyRate: Double,
    val totalDays: Int,
    val optionsCost: Double = 0.0,
    val options: String = "",
    val totalPrice: Double,
    val status: String = RentalStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contracts")
data class Contract(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reservationId: Int,
    val number: String,
    val startMileage: Int,
    val endMileage: Int? = null,
    val depositAmount: Double,
    val startFuel: Int = 100,
    val endFuel: Int? = null,
    val status: String = ContractStatus.PENDING,
    val signedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reservationId: Int,
    val contractId: Int,
    val number: String,
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val paymentStatus: String = PaymentStatus.PENDING,
    val paymentMethod: String = "Espèces",
    val issuedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "maintenance_records")
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val description: String,
    val cost: Double,
    val date: Long = System.currentTimeMillis(),
    val status: String = "Ouverte"
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val description: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis()
)
