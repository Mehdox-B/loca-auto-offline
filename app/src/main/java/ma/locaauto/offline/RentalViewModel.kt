package ma.locaauto.offline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ma.locaauto.offline.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RentalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RentalRepository(AppDatabase.get(application, viewModelScope))

    val cars = repository.cars.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val clients = repository.clients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reservations = repository.reservations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val contracts = repository.contracts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val invoices = repository.invoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val maintenance = repository.maintenance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val monthlyRevenue = invoices.map(repository::monthlyRevenue).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    fun addCar(brand: String, model: String, category: String, transmission: String, fuel: String, rate: Double, plate: String, mileage: Int) = launchAction("Véhicule ajouté à la flotte") {
        repository.addCar(Car(brand = brand, model = model, category = category, transmission = transmission, fuelType = fuel, dailyRate = rate, licensePlate = plate, mileage = mileage))
    }

    fun updateCar(car: Car) = launchResultAction("Véhicule mis à jour") { repository.updateCar(car) }

    fun deleteCar(id: Int) = launchResultAction("Véhicule supprimé") { repository.deleteCar(id) }

    fun updateCarStatus(carId: Int, status: String) = launchResultAction("Statut du véhicule mis à jour") {
        repository.updateCarStatus(carId, status)
    }

    fun addClient(name: String, phone: String, email: String, license: String, identity: String, address: String) = launchAction("Client enregistré") {
        repository.addClient(Client(fullName = name, phone = phone, email = email, driverLicenseNumber = license, identityNumber = identity, address = address))
    }

    fun updateClient(client: Client) = launchResultAction("Client mis à jour") { repository.updateClient(client) }

    fun deleteClient(id: Int) = launchResultAction("Client supprimé") { repository.deleteClient(id) }

    fun addReservation(carId: Int, clientId: Int, days: Int, options: String, optionsCost: Double) {
        viewModelScope.launch {
            val start = System.currentTimeMillis() + 86_400_000L
            val end = start + days.coerceAtLeast(1) * 86_400_000L
            val result = repository.createReservation(carId, clientId, start, end, options, optionsCost)
            _message.value = result.fold({ "Réservation #$it créée" }, { it.message ?: "Impossible de créer la réservation" })
        }
    }

    fun updateReservation(id: Int, carId: Int, clientId: Int, startDate: Long, days: Int, options: String, optionsCost: Double) {
        viewModelScope.launch {
            val end = startDate + days.coerceAtLeast(1) * 86_400_000L
            val result = repository.updateReservation(id, carId, clientId, startDate, end, options, optionsCost)
            _message.value = result.fold({ "Réservation mise à jour" }, { it.message ?: "Mise à jour impossible" })
        }
    }

    fun updateReservationStatus(id: Int, status: String) {
        viewModelScope.launch {
            val result = repository.updateReservationStatus(id, status)
            _message.value = result.fold({ "Réservation mise à jour" }, { it.message ?: "Mise à jour impossible" })
        }
    }

    fun deleteReservation(id: Int) = launchResultAction("Réservation supprimée") { repository.deleteReservation(id) }

    fun updateContractStatus(id: Int, status: String) {
        viewModelScope.launch {
            val result = repository.updateContractStatus(id, status)
            _message.value = result.fold({ "Contrat mis à jour" }, { it.message ?: "Mise à jour impossible" })
        }
    }

    fun createContract(reservationId: Int) = launchResultAction("Contrat créé") { repository.createContract(reservationId) }

    fun createInvoice(contractId: Int) = launchResultAction("Facture créée") { repository.createInvoice(contractId) }

    fun updateInvoiceStatus(id: Int, status: String, method: String) = launchAction("Paiement mis à jour") {
        repository.updateInvoiceStatus(id, status, method).getOrThrow()
    }

    fun deleteContract(id: Int) = launchResultAction("Contrat supprimé") { repository.deleteContract(id) }

    fun deleteInvoice(id: Int) = launchResultAction("Facture supprimée") { repository.deleteInvoice(id) }

    fun addMaintenance(carId: Int, description: String, cost: Double) = launchAction("Entretien enregistré") {
        repository.addMaintenance(MaintenanceRecord(carId = carId, description = description, cost = cost)).getOrThrow()
    }

    fun addExpense(category: String, description: String, amount: Double) = launchAction("Dépense enregistrée") {
        repository.addExpense(Expense(category = category, description = description, amount = amount))
    }

    private fun launchAction(success: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }.onSuccess { _message.value = success }.onFailure { _message.value = it.message ?: "Une erreur est survenue" }
        }
    }

    private fun <T> launchResultAction(success: String, action: suspend () -> Result<T>) {
        viewModelScope.launch {
            val result = runCatching { action() }.getOrElse { Result.failure(it) }
            _message.value = result.fold({ success }, { it.message ?: "Une erreur est survenue" })
        }
    }
}
