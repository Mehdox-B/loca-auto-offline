package ma.locaauto.offline.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.ceil
import kotlin.random.Random

class RentalRepository(private val database: AppDatabase) {
    private val dao = database.rentalDao()

    val cars: Flow<List<Car>> = dao.observeCars()
    val clients: Flow<List<Client>> = dao.observeClients()
    val reservations: Flow<List<Reservation>> = dao.observeReservations()
    val contracts: Flow<List<Contract>> = dao.observeContracts()
    val invoices: Flow<List<Invoice>> = dao.observeInvoices()
    val maintenance: Flow<List<MaintenanceRecord>> = dao.observeMaintenance()
    val expenses: Flow<List<Expense>> = dao.observeExpenses()

    suspend fun addCar(car: Car) = dao.insertCar(car)
    suspend fun addClient(client: Client) = dao.insertClient(client)
    suspend fun addExpense(expense: Expense) = dao.insertExpense(expense)
    suspend fun addMaintenance(record: MaintenanceRecord) = dao.insertMaintenance(record)

    suspend fun createReservation(
        carId: Int,
        clientId: Int,
        startDate: Long,
        endDate: Long,
        options: String,
        optionsCost: Double
    ): Result<Long> = database.withTransaction {
        val car = dao.getCar(carId) ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))
        if (car.status == CarStatus.MAINTENANCE) {
            return@withTransaction Result.failure(IllegalStateException("Ce véhicule est en maintenance"))
        }
        if (endDate <= startDate) {
            return@withTransaction Result.failure(IllegalArgumentException("Les dates sont invalides"))
        }
        if (dao.countOverlappingReservations(carId, startDate, endDate) > 0) {
            return@withTransaction Result.failure(IllegalStateException("Ce véhicule est déjà réservé sur cette période"))
        }
        val totalDays = ceil((endDate - startDate).toDouble() / 86_400_000.0).toInt().coerceAtLeast(1)
        val total = car.dailyRate * totalDays + optionsCost
        val reservation = Reservation(
            carId = carId,
            clientId = clientId,
            startDate = startDate,
            endDate = endDate,
            dailyRate = car.dailyRate,
            totalDays = totalDays,
            optionsCost = optionsCost,
            options = options,
            totalPrice = total
        )
        Result.success(dao.insertReservation(reservation))
    }

    suspend fun updateCarStatus(carId: Int, status: String) {
        val car = dao.getCar(carId) ?: return
        dao.updateCar(car.copy(status = status))
    }

    suspend fun updateReservationStatus(reservationId: Int, status: String): Result<Unit> = database.withTransaction {
        val reservation = dao.getReservation(reservationId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Réservation introuvable"))
        val car = dao.getCar(reservation.carId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))

        if (status == RentalStatus.CONFIRMED || status == RentalStatus.ACTIVE) {
            if (status == RentalStatus.ACTIVE && dao.countOverlappingReservations(car.id, reservation.startDate, reservation.endDate) > 1) {
                return@withTransaction Result.failure(IllegalStateException("Impossible de démarrer : conflit de réservation"))
            }
            if (dao.getContractForReservation(reservationId) == null) {
                dao.insertContract(
                    Contract(
                        reservationId = reservationId,
                        number = "CTR-${Calendar.getInstance().get(Calendar.YEAR)}-${Random.nextInt(1000, 9999)}",
                        startMileage = car.mileage,
                        depositAmount = if (car.category == "Premium") 1_500.0 else 800.0
                    )
                )
            }
        }

        dao.updateReservation(reservation.copy(status = status))
        when (status) {
            RentalStatus.ACTIVE -> dao.updateCar(car.copy(status = CarStatus.RENTED))
            RentalStatus.COMPLETED, RentalStatus.CANCELLED -> dao.updateCar(car.copy(status = CarStatus.AVAILABLE))
        }
        Result.success(Unit)
    }

    suspend fun deleteReservation(reservationId: Int) {
        dao.getReservation(reservationId)?.let { dao.deleteReservation(it) }
    }

    suspend fun updateContractStatus(contractId: Int, status: String): Result<Unit> = database.withTransaction {
        val contract = dao.getContract(contractId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Contrat introuvable"))
        dao.updateContract(contract.copy(status = status))
        if ((status == ContractStatus.SIGNED || status == ContractStatus.ACTIVE) && dao.getInvoiceForContract(contractId) == null) {
            val reservation = dao.getReservation(contract.reservationId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Réservation introuvable"))
            val subtotal = reservation.totalPrice / 1.2
            dao.insertInvoice(
                Invoice(
                    reservationId = reservation.id,
                    contractId = contractId,
                    number = "FAC-${Calendar.getInstance().get(Calendar.YEAR)}-${Random.nextInt(1000, 9999)}",
                    subtotal = subtotal,
                    tax = reservation.totalPrice - subtotal,
                    total = reservation.totalPrice
                )
            )
        }
        Result.success(Unit)
    }

    suspend fun updateInvoiceStatus(invoiceId: Int, status: String, method: String? = null) {
        dao.getInvoice(invoiceId)?.let { invoice ->
            dao.updateInvoice(invoice.copy(paymentStatus = status, paymentMethod = method ?: invoice.paymentMethod))
        }
    }

    fun monthlyRevenue(invoices: List<Invoice>): List<Pair<String, Double>> {
        val monthNames = listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc")
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return monthNames.mapIndexed { index, name ->
            val total = invoices.filter { invoice ->
                invoice.paymentStatus == PaymentStatus.PAID &&
                    Calendar.getInstance().apply { timeInMillis = invoice.issuedAt }.get(Calendar.YEAR) == currentYear &&
                    Calendar.getInstance().apply { timeInMillis = invoice.issuedAt }.get(Calendar.MONTH) == index
            }.sumOf { it.total }
            name to total
        }
    }
}
