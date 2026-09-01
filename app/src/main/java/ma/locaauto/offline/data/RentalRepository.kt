package ma.locaauto.offline.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.ceil
import kotlin.random.Random

/** All write operations live here so related records change atomically. */
class RentalRepository(private val database: AppDatabase) {
    private val dao = database.rentalDao()

    val cars: Flow<List<Car>> = dao.observeCars()
    val clients: Flow<List<Client>> = dao.observeClients()
    val reservations: Flow<List<Reservation>> = dao.observeReservations()
    val contracts: Flow<List<Contract>> = dao.observeContracts()
    val invoices: Flow<List<Invoice>> = dao.observeInvoices()
    val maintenance: Flow<List<MaintenanceRecord>> = dao.observeMaintenance()
    val expenses: Flow<List<Expense>> = dao.observeExpenses()

    suspend fun addCar(car: Car): Long = database.withTransaction {
        validateCar(car)
        ensurePlateIsUnique(car.licensePlate)
        dao.insertCar(car.copy(licensePlate = car.licensePlate.trim().uppercase()))
    }

    suspend fun updateCar(car: Car): Result<Unit> = database.withTransaction {
        val current = dao.getCar(car.id)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))
        validateCar(car)
        ensurePlateIsUnique(car.licensePlate, car.id)
        if (car.status == CarStatus.MAINTENANCE && dao.countOpenReservationsForCar(car.id) > 0) {
            return@withTransaction Result.failure(IllegalStateException("Un véhicule avec une réservation ouverte ne peut pas être envoyé en maintenance"))
        }
        if (car.status == CarStatus.RENTED && dao.countActiveReservationsForCar(car.id) == 0) {
            return@withTransaction Result.failure(IllegalStateException("Un véhicule ne peut être marqué loué sans réservation active"))
        }
        if (current.status == CarStatus.RENTED && car.status != CarStatus.RENTED && dao.countActiveReservationsForCar(car.id) > 0) {
            return@withTransaction Result.failure(IllegalStateException("La réservation active doit être clôturée avant de libérer ce véhicule"))
        }
        dao.updateCar(car.copy(licensePlate = car.licensePlate.trim().uppercase()))
        Result.success(Unit)
    }

    suspend fun deleteCar(carId: Int): Result<Unit> = database.withTransaction {
        val car = dao.getCar(carId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))
        if (dao.countReservationsForCar(carId) > 0) {
            return@withTransaction Result.failure(IllegalStateException("Impossible de supprimer un véhicule lié à une réservation"))
        }
        if (dao.countMaintenanceForCar(carId) > 0) {
            return@withTransaction Result.failure(IllegalStateException("Impossible de supprimer un véhicule lié à un entretien"))
        }
        dao.deleteCar(car)
        Result.success(Unit)
    }

    suspend fun addClient(client: Client): Long = database.withTransaction {
        validateClient(client)
        dao.insertClient(client)
    }

    suspend fun updateClient(client: Client): Result<Unit> = database.withTransaction {
        if (dao.getClient(client.id) == null) {
            return@withTransaction Result.failure(IllegalArgumentException("Client introuvable"))
        }
        validateClient(client)
        dao.updateClient(client)
        Result.success(Unit)
    }

    suspend fun deleteClient(clientId: Int): Result<Unit> = database.withTransaction {
        val client = dao.getClient(clientId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Client introuvable"))
        if (dao.countReservationsForClient(clientId) > 0) {
            return@withTransaction Result.failure(IllegalStateException("Impossible de supprimer un client lié à une réservation"))
        }
        dao.deleteClient(client)
        Result.success(Unit)
    }

    suspend fun addExpense(expense: Expense): Long = database.withTransaction {
        require(expense.category.isNotBlank() && expense.description.isNotBlank()) { "Les informations de la dépense sont obligatoires" }
        require(expense.amount > 0.0) { "Le montant doit être supérieur à zéro" }
        dao.insertExpense(expense)
    }

    suspend fun addMaintenance(record: MaintenanceRecord): Result<Unit> = database.withTransaction {
        val car = dao.getCar(record.carId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))
        if (car.status == CarStatus.RENTED || dao.countOpenReservationsForCar(record.carId) > 0) {
            return@withTransaction Result.failure(IllegalStateException("Un véhicule avec une réservation ouverte ne peut pas être mis en maintenance"))
        }
        require(record.description.isNotBlank()) { "La description de l'entretien est obligatoire" }
        require(record.cost >= 0.0) { "Le coût de l'entretien est invalide" }
        dao.insertMaintenance(record)
        dao.updateCar(car.copy(status = CarStatus.MAINTENANCE))
        Result.success(Unit)
    }

    suspend fun createReservation(carId: Int, clientId: Int, startDate: Long, endDate: Long, options: String, optionsCost: Double): Result<Long> = database.withTransaction {
        val car = dao.getCar(carId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))
        if (dao.getClient(clientId) == null) {
            return@withTransaction Result.failure(IllegalArgumentException("Client introuvable"))
        }
        validateReservationDates(startDate, endDate, optionsCost)
        if (car.status == CarStatus.MAINTENANCE) {
            return@withTransaction Result.failure(IllegalStateException("Ce véhicule est en maintenance"))
        }
        if (dao.countOverlappingReservations(carId, startDate, endDate) > 0) {
            return@withTransaction Result.failure(IllegalStateException("Ce véhicule est déjà réservé sur cette période"))
        }
        val totalDays = rentalDays(startDate, endDate)
        val reservation = Reservation(carId = carId, clientId = clientId, startDate = startDate, endDate = endDate, dailyRate = car.dailyRate, totalDays = totalDays, optionsCost = optionsCost, options = options.trim(), totalPrice = car.dailyRate * totalDays + optionsCost)
        Result.success(dao.insertReservation(reservation))
    }

    suspend fun updateReservation(reservationId: Int, carId: Int, clientId: Int, startDate: Long, endDate: Long, options: String, optionsCost: Double): Result<Unit> = database.withTransaction {
        val current = dao.getReservation(reservationId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Réservation introuvable"))
        if (current.status == RentalStatus.ACTIVE || current.status == RentalStatus.COMPLETED) {
            return@withTransaction Result.failure(IllegalStateException("Cette réservation n'est plus modifiable"))
        }
        val car = dao.getCar(carId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))
        if (dao.getClient(clientId) == null) {
            return@withTransaction Result.failure(IllegalArgumentException("Client introuvable"))
        }
        validateReservationDates(startDate, endDate, optionsCost)
        if (car.status == CarStatus.MAINTENANCE) {
            return@withTransaction Result.failure(IllegalStateException("Ce véhicule est en maintenance"))
        }
        val currentInvoice = dao.getContractForReservation(reservationId)?.let { dao.getInvoiceForContract(it.id) }
        if (currentInvoice?.paymentStatus == PaymentStatus.PAID) {
            return@withTransaction Result.failure(IllegalStateException("Une réservation avec une facture payée ne peut pas être modifiée"))
        }
        val conflicts = dao.countOverlappingReservations(carId, startDate, endDate)
        val currentIsTheOnlyConflict = current.carId == carId && current.startDate < endDate && current.endDate > startDate
        if (conflicts > 0 && !currentIsTheOnlyConflict) {
            return@withTransaction Result.failure(IllegalStateException("Ce véhicule est déjà réservé sur cette période"))
        }
        val totalDays = rentalDays(startDate, endDate)
        val updated = current.copy(carId = carId, clientId = clientId, startDate = startDate, endDate = endDate, dailyRate = car.dailyRate, totalDays = totalDays, optionsCost = optionsCost, options = options.trim(), totalPrice = car.dailyRate * totalDays + optionsCost)
        dao.updateReservation(updated)
        dao.getContractForReservation(reservationId)?.let { contract ->
            dao.getInvoiceForContract(contract.id)?.let { invoice ->
                val subtotal = updated.totalPrice / 1.2
                dao.updateInvoice(invoice.copy(subtotal = subtotal, tax = updated.totalPrice - subtotal, total = updated.totalPrice))
            }
        }
        Result.success(Unit)
    }

    suspend fun updateCarStatus(carId: Int, status: String): Result<Unit> = database.withTransaction {
        if (status !in listOf(CarStatus.AVAILABLE, CarStatus.RENTED, CarStatus.MAINTENANCE)) {
            return@withTransaction Result.failure(IllegalArgumentException("Statut de véhicule invalide"))
        }
        val car = dao.getCar(carId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))
        if (status == CarStatus.MAINTENANCE && dao.countOpenReservationsForCar(carId) > 0) {
            return@withTransaction Result.failure(IllegalStateException("Un véhicule avec une réservation ouverte ne peut pas être mis en maintenance"))
        }
        if (status == CarStatus.RENTED && dao.countActiveReservationsForCar(carId) == 0) {
            return@withTransaction Result.failure(IllegalStateException("Un véhicule ne peut être marqué loué sans réservation active"))
        }
        if (status == CarStatus.AVAILABLE && dao.countActiveReservationsForCar(carId) > 0) {
            return@withTransaction Result.failure(IllegalStateException("La réservation active doit être clôturée avant de libérer ce véhicule"))
        }
        dao.updateCar(car.copy(status = status))
        Result.success(Unit)
    }

    suspend fun updateReservationStatus(reservationId: Int, status: String): Result<Unit> = database.withTransaction {
        if (status !in listOf(RentalStatus.PENDING, RentalStatus.CONFIRMED, RentalStatus.ACTIVE, RentalStatus.COMPLETED, RentalStatus.CANCELLED)) {
            return@withTransaction Result.failure(IllegalArgumentException("Statut de réservation invalide"))
        }
        val reservation = dao.getReservation(reservationId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Réservation introuvable"))
        val car = dao.getCar(reservation.carId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))
        if (status == RentalStatus.CONFIRMED || status == RentalStatus.ACTIVE) {
            if (car.status == CarStatus.MAINTENANCE) {
                return@withTransaction Result.failure(IllegalStateException("Ce véhicule est en maintenance"))
            }
            if (dao.countOverlappingReservations(car.id, reservation.startDate, reservation.endDate) > 1) {
                return@withTransaction Result.failure(IllegalStateException("Impossible de valider : conflit de réservation"))
            }
            if (dao.getContractForReservation(reservationId) == null) {
                dao.insertContract(Contract(reservationId = reservationId, number = "CTR-${Calendar.getInstance().get(Calendar.YEAR)}-${Random.nextInt(1000, 9999)}", startMileage = car.mileage, depositAmount = if (car.category == "Premium") 1_500.0 else 800.0))
            }
        }
        dao.updateReservation(reservation.copy(status = status))
        syncCarStatus(car)
        Result.success(Unit)
    }

    suspend fun deleteReservation(reservationId: Int): Result<Unit> = database.withTransaction {
        val reservation = dao.getReservation(reservationId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Réservation introuvable"))
        dao.getContractForReservation(reservationId)?.let { contract ->
            dao.getInvoiceForContract(contract.id)?.let { dao.deleteInvoice(it) }
            dao.deleteContract(contract)
        }
        dao.deleteReservation(reservation)
        dao.getCar(reservation.carId)?.let { syncCarStatus(it) }
        Result.success(Unit)
    }

    suspend fun updateContractStatus(contractId: Int, status: String): Result<Unit> = database.withTransaction {
        if (status !in listOf(ContractStatus.PENDING, ContractStatus.SIGNED, ContractStatus.ACTIVE, ContractStatus.CLOSED, ContractStatus.CANCELLED)) {
            return@withTransaction Result.failure(IllegalArgumentException("Statut de contrat invalide"))
        }
        val contract = dao.getContract(contractId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Contrat introuvable"))
        val reservation = dao.getReservation(contract.reservationId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Réservation introuvable"))
        dao.updateContract(contract.copy(status = status))
        if ((status == ContractStatus.SIGNED || status == ContractStatus.ACTIVE) && dao.getInvoiceForContract(contractId) == null) {
            val subtotal = reservation.totalPrice / 1.2
            dao.insertInvoice(Invoice(reservationId = reservation.id, contractId = contractId, number = "FAC-${Calendar.getInstance().get(Calendar.YEAR)}-${Random.nextInt(1000, 9999)}", subtotal = subtotal, tax = reservation.totalPrice - subtotal, total = reservation.totalPrice))
        }
        Result.success(Unit)
    }

    /** Explicit document creation APIs used by the reservation workflow. */
    suspend fun createContract(reservationId: Int): Result<Long> = database.withTransaction {
        val reservation = dao.getReservation(reservationId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Réservation introuvable"))
        if (reservation.status != RentalStatus.CONFIRMED && reservation.status != RentalStatus.ACTIVE) {
            return@withTransaction Result.failure(IllegalStateException("La réservation doit être confirmée avant la création du contrat"))
        }
        if (dao.getContractForReservation(reservationId) != null) {
            return@withTransaction Result.failure(IllegalStateException("Un contrat existe déjà pour cette réservation"))
        }
        val car = dao.getCar(reservation.carId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Véhicule introuvable"))
        Result.success(dao.insertContract(Contract(reservationId = reservationId, number = nextNumber("CTR"), startMileage = car.mileage, depositAmount = if (car.category == "Premium") 1_500.0 else 800.0)))
    }

    suspend fun createInvoice(contractId: Int): Result<Long> = database.withTransaction {
        val contract = dao.getContract(contractId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Contrat introuvable"))
        if (contract.status != ContractStatus.SIGNED && contract.status != ContractStatus.ACTIVE) {
            return@withTransaction Result.failure(IllegalStateException("Le contrat doit être signé avant la création de la facture"))
        }
        if (dao.getInvoiceForContract(contractId) != null) {
            return@withTransaction Result.failure(IllegalStateException("Une facture existe déjà pour ce contrat"))
        }
        val reservation = dao.getReservation(contract.reservationId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Réservation introuvable"))
        val subtotal = reservation.totalPrice / 1.2
        Result.success(dao.insertInvoice(Invoice(reservationId = reservation.id, contractId = contractId, number = nextNumber("FAC"), subtotal = subtotal, tax = reservation.totalPrice - subtotal, total = reservation.totalPrice)))
    }

    suspend fun updateContract(contract: Contract): Result<Unit> = database.withTransaction {
        val current = dao.getContract(contract.id)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Contrat introuvable"))
        if (current.reservationId != contract.reservationId) {
            return@withTransaction Result.failure(IllegalArgumentException("La réservation d'un contrat ne peut pas être changée"))
        }
        if (contract.status !in listOf(ContractStatus.PENDING, ContractStatus.SIGNED, ContractStatus.ACTIVE, ContractStatus.CLOSED, ContractStatus.CANCELLED)) {
            return@withTransaction Result.failure(IllegalArgumentException("Statut de contrat invalide"))
        }
        dao.updateContract(contract)
        Result.success(Unit)
    }

    suspend fun updateInvoice(invoice: Invoice): Result<Unit> = database.withTransaction {
        val current = dao.getInvoice(invoice.id)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Facture introuvable"))
        if (current.contractId != invoice.contractId || current.reservationId != invoice.reservationId) {
            return@withTransaction Result.failure(IllegalArgumentException("Les liens d'une facture ne peuvent pas être changés"))
        }
        if (invoice.paymentStatus !in listOf(PaymentStatus.PENDING, PaymentStatus.PAID, PaymentStatus.LATE, PaymentStatus.CANCELLED)) {
            return@withTransaction Result.failure(IllegalArgumentException("Statut de paiement invalide"))
        }
        require(invoice.subtotal >= 0.0 && invoice.tax >= 0.0 && invoice.total > 0.0) { "Les montants de la facture sont invalides" }
        dao.updateInvoice(invoice)
        Result.success(Unit)
    }

    suspend fun deleteContract(contractId: Int): Result<Unit> = database.withTransaction {
        val contract = dao.getContract(contractId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Contrat introuvable"))
        val reservation = dao.getReservation(contract.reservationId)
        if (reservation?.status == RentalStatus.ACTIVE) {
            return@withTransaction Result.failure(IllegalStateException("Un contrat actif ne peut pas être supprimé"))
        }
        dao.getInvoiceForContract(contractId)?.let { dao.deleteInvoice(it) }
        dao.deleteContract(contract)
        if (reservation?.status == RentalStatus.CONFIRMED) {
            dao.updateReservation(reservation.copy(status = RentalStatus.PENDING))
            dao.getCar(reservation.carId)?.let { syncCarStatus(it) }
        }
        Result.success(Unit)
    }

    suspend fun updateInvoiceStatus(invoiceId: Int, status: String, method: String? = null): Result<Unit> = database.withTransaction {
        if (status !in listOf(PaymentStatus.PENDING, PaymentStatus.PAID, PaymentStatus.LATE, PaymentStatus.CANCELLED)) {
            return@withTransaction Result.failure(IllegalArgumentException("Statut de paiement invalide"))
        }
        val invoice = dao.getInvoice(invoiceId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Facture introuvable"))
        dao.updateInvoice(invoice.copy(paymentStatus = status, paymentMethod = method ?: invoice.paymentMethod))
        Result.success(Unit)
    }

    suspend fun deleteInvoice(invoiceId: Int): Result<Unit> = database.withTransaction {
        val invoice = dao.getInvoice(invoiceId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Facture introuvable"))
        if (invoice.paymentStatus == PaymentStatus.PAID) {
            return@withTransaction Result.failure(IllegalStateException("Une facture payée ne peut pas être supprimée"))
        }
        dao.deleteInvoice(invoice)
        Result.success(Unit)
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

    private suspend fun syncCarStatus(car: Car) {
        if (car.status == CarStatus.MAINTENANCE) return
        val desired = if (dao.countActiveReservationsForCar(car.id) > 0) CarStatus.RENTED else CarStatus.AVAILABLE
        if (car.status != desired) dao.updateCar(car.copy(status = desired))
    }

    private suspend fun ensurePlateIsUnique(plate: String, excludedId: Int = 0) {
        if (dao.countCarsWithPlate(plate.trim().uppercase(), excludedId) > 0) {
            throw IllegalArgumentException("Cette immatriculation est déjà utilisée")
        }
    }

    private fun nextNumber(prefix: String) = "$prefix-${Calendar.getInstance().get(Calendar.YEAR)}-${Random.nextInt(1000, 9999)}"

    private fun validateCar(car: Car) {
        require(car.brand.isNotBlank() && car.model.isNotBlank()) { "La marque et le modèle sont obligatoires" }
        require(car.licensePlate.isNotBlank()) { "L'immatriculation est obligatoire" }
        require(car.dailyRate > 0.0) { "Le tarif journalier doit être supérieur à zéro" }
        require(car.seats > 0 && car.mileage >= 0 && car.year > 1900) { "Les caractéristiques du véhicule sont invalides" }
    }

    private fun validateClient(client: Client) {
        require(client.fullName.isNotBlank() && client.phone.isNotBlank()) { "Le nom et le téléphone sont obligatoires" }
    }

    private fun validateReservationDates(startDate: Long, endDate: Long, optionsCost: Double) {
        require(endDate > startDate) { "Les dates sont invalides" }
        require(optionsCost >= 0.0) { "Le coût des options est invalide" }
    }

    private fun rentalDays(startDate: Long, endDate: Long) = ceil((endDate - startDate).toDouble() / 86_400_000.0).toInt().coerceAtLeast(1)
}
