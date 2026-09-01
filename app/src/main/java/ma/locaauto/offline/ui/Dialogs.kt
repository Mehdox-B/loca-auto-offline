package ma.locaauto.offline.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import ma.locaauto.offline.data.Car
import ma.locaauto.offline.data.CarStatus
import ma.locaauto.offline.data.Client

@Composable
fun AddCarDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String, String, Double, String, Int) -> Unit) {
    var brand by remember { mutableStateOf("") }; var model by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Citadine") }
    var transmission by remember { mutableStateOf("Manuelle") }; var fuel by remember { mutableStateOf("Essence") }; var rate by remember { mutableStateOf("250") }
    var plate by remember { mutableStateOf("") }; var mileage by remember { mutableStateOf("0") }
    FormDialog("Ajouter un véhicule", onDismiss, enabled = brand.isNotBlank() && model.isNotBlank() && plate.isNotBlank(), confirmLabel = "Ajouter", content = {
        Field("Marque", brand) { brand = it }; Field("Modèle", model) { model = it }; Field("Immatriculation", plate) { plate = it }
        Field("Tarif journalier (MAD)", rate, KeyboardType.Decimal) { rate = it }; Field("Kilométrage", mileage, KeyboardType.Number) { mileage = it }
        ChoiceRow("Catégorie", listOf("Citadine", "Économique", "SUV", "Premium", "Utilitaire"), category) { category = it }
        ChoiceRow("Boîte", listOf("Manuelle", "Automatique"), transmission) { transmission = it }
        ChoiceRow("Énergie", listOf("Essence", "Diesel", "Hybride", "Électrique"), fuel) { fuel = it }
    }, onSubmit = {
        onConfirm(brand, model, category, transmission, fuel, rate.toDoubleOrNull() ?: 0.0, plate, mileage.toIntOrNull() ?: 0)
    })
}

@Composable
fun EditCarDialog(car: Car, onDismiss: () -> Unit, onConfirm: (Car) -> Unit) {
    var brand by remember(car.id) { mutableStateOf(car.brand) }; var model by remember(car.id) { mutableStateOf(car.model) }
    var category by remember(car.id) { mutableStateOf(car.category) }; var transmission by remember(car.id) { mutableStateOf(car.transmission) }
    var fuel by remember(car.id) { mutableStateOf(car.fuelType) }; var rate by remember(car.id) { mutableStateOf(car.dailyRate.toString()) }
    var plate by remember(car.id) { mutableStateOf(car.licensePlate) }; var mileage by remember(car.id) { mutableStateOf(car.mileage.toString()) }
    FormDialog("Modifier le véhicule", onDismiss, enabled = brand.isNotBlank() && model.isNotBlank() && plate.isNotBlank() && rate.toDoubleOrNull()?.let { it > 0 } == true, confirmLabel = "Enregistrer", content = {
        Field("Marque", brand) { brand = it }; Field("Modèle", model) { model = it }; Field("Immatriculation", plate) { plate = it }
        Field("Tarif journalier (MAD)", rate, KeyboardType.Decimal) { rate = it }; Field("Kilométrage", mileage, KeyboardType.Number) { mileage = it }
        ChoiceRow("Catégorie", listOf("Citadine", "Économique", "SUV", "Premium", "Utilitaire"), category) { category = it }
        ChoiceRow("Boîte", listOf("Manuelle", "Automatique"), transmission) { transmission = it }
        ChoiceRow("Énergie", listOf("Essence", "Diesel", "Hybride", "Électrique"), fuel) { fuel = it }
    }, onSubmit = {
        onConfirm(car.copy(brand = brand.trim(), model = model.trim(), category = category, transmission = transmission, fuelType = fuel, dailyRate = rate.toDoubleOrNull() ?: car.dailyRate, licensePlate = plate.trim(), mileage = mileage.toIntOrNull() ?: car.mileage))
    })
}

@Composable
fun AddClientDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String, String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }; var identity by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }
    var nationalIdDocumentUri by remember { mutableStateOf("") }; var driverLicenseDocumentUri by remember { mutableStateOf("") }
    FormDialog("Ajouter un client", onDismiss, enabled = name.isNotBlank() && phone.isNotBlank(), confirmLabel = "Enregistrer", content = {
        Field("Nom complet", name) { name = it }; Field("Téléphone", phone, KeyboardType.Phone) { phone = it }; Field("E-mail", email, KeyboardType.Email) { email = it }
        Field("N° permis", license) { license = it }; Field("CIN / Passeport", identity) { identity = it }; Field("Ville / adresse", address) { address = it }
        DocumentPickerField("Carte Nationale", nationalIdDocumentUri) { nationalIdDocumentUri = it }
        DocumentPickerField("Permis de conduite", driverLicenseDocumentUri) { driverLicenseDocumentUri = it }
    }, onSubmit = { onConfirm(name, phone, email, license, identity, address, nationalIdDocumentUri, driverLicenseDocumentUri) })
}

@Composable
fun EditClientDialog(client: Client, onDismiss: () -> Unit, onConfirm: (Client) -> Unit) {
    var name by remember(client.id) { mutableStateOf(client.fullName) }; var phone by remember(client.id) { mutableStateOf(client.phone) }
    var email by remember(client.id) { mutableStateOf(client.email) }; var license by remember(client.id) { mutableStateOf(client.driverLicenseNumber) }
    var identity by remember(client.id) { mutableStateOf(client.identityNumber) }; var address by remember(client.id) { mutableStateOf(client.address) }
    var nationalIdDocumentUri by remember(client.id) { mutableStateOf(client.nationalIdDocumentUri) }; var driverLicenseDocumentUri by remember(client.id) { mutableStateOf(client.driverLicenseDocumentUri) }
    FormDialog("Modifier le client", onDismiss, enabled = name.isNotBlank() && phone.isNotBlank(), confirmLabel = "Enregistrer", content = {
        Field("Nom complet", name) { name = it }; Field("Téléphone", phone, KeyboardType.Phone) { phone = it }; Field("E-mail", email, KeyboardType.Email) { email = it }
        Field("N° permis", license) { license = it }; Field("CIN / Passeport", identity) { identity = it }; Field("Ville / adresse", address) { address = it }
        DocumentPickerField("Carte Nationale", nationalIdDocumentUri) { nationalIdDocumentUri = it }
        DocumentPickerField("Permis de conduite", driverLicenseDocumentUri) { driverLicenseDocumentUri = it }
    }, onSubmit = { onConfirm(client.copy(fullName = name.trim(), phone = phone.trim(), email = email.trim(), driverLicenseNumber = license.trim(), identityNumber = identity.trim(), address = address.trim(), nationalIdDocumentUri = nationalIdDocumentUri, driverLicenseDocumentUri = driverLicenseDocumentUri)) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReservationDialog(cars: List<Car>, clients: List<Client>, onDismiss: () -> Unit, onConfirm: (Int, Int, Int, String, Double) -> Unit) {
    val selectableCars = cars.filter { it.status != CarStatus.MAINTENANCE }
    var selectedCar by remember { mutableStateOf(selectableCars.firstOrNull()) }; var selectedClient by remember { mutableStateOf(clients.firstOrNull()) }
    var carExpanded by remember { mutableStateOf(false) }; var clientExpanded by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf("1") }; var options by remember { mutableStateOf("") }; var optionsCost by remember { mutableStateOf("0") }
    FormDialog("Nouvelle réservation", onDismiss, enabled = selectedCar != null && selectedClient != null && days.toIntOrNull()?.let { it > 0 } == true, confirmLabel = "Créer", content = {
        ExposedDropdownMenuBox(expanded = carExpanded, onExpandedChange = { carExpanded = !carExpanded }) {
            OutlinedTextField(selectedCar?.let { "${it.brand} ${it.model}" } ?: "Aucun véhicule", {}, Modifier.menuAnchor().fillMaxWidth(), readOnly = true, label = { Text("Véhicule") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
            ExposedDropdownMenu(expanded = carExpanded, onDismissRequest = { carExpanded = false }) { selectableCars.forEach { car -> DropdownMenuItem(text = { Text("${car.brand} ${car.model} • ${car.dailyRate.toInt()} MAD/j") }, onClick = { selectedCar = car; carExpanded = false }) } }
        }
        ExposedDropdownMenuBox(expanded = clientExpanded, onExpandedChange = { clientExpanded = !clientExpanded }) {
            OutlinedTextField(selectedClient?.fullName ?: "Aucun client", {}, Modifier.menuAnchor().fillMaxWidth(), readOnly = true, label = { Text("Client") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
            ExposedDropdownMenu(expanded = clientExpanded, onDismissRequest = { clientExpanded = false }) { clients.forEach { client -> DropdownMenuItem(text = { Text(client.fullName) }, onClick = { selectedClient = client; clientExpanded = false }) } }
        }
        Field("Durée (jours)", days, KeyboardType.Number) { days = it }; Field("Options", options) { options = it }; Field("Coût des options (MAD)", optionsCost, KeyboardType.Decimal) { optionsCost = it }
        Text("La date de départ est fixée à demain. Le dépôt et la facture seront générés selon le statut.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }, onSubmit = { onConfirm(selectedCar!!.id, selectedClient!!.id, days.toIntOrNull() ?: 1, options, optionsCost.toDoubleOrNull() ?: 0.0) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReservationDialog(reservation: Reservation, cars: List<Car>, clients: List<Client>, onDismiss: () -> Unit, onConfirm: (Int, Int, Long, Int, String, Double) -> Unit) {
    val currentCar = cars.find { it.id == reservation.carId }
    val selectableCars = cars.filter { it.status != CarStatus.MAINTENANCE || it.id == reservation.carId }
    var selectedCar by remember(reservation.id) { mutableStateOf(currentCar ?: selectableCars.firstOrNull()) }
    var selectedClient by remember(reservation.id) { mutableStateOf(clients.find { it.id == reservation.clientId } ?: clients.firstOrNull()) }
    var carExpanded by remember { mutableStateOf(false) }; var clientExpanded by remember { mutableStateOf(false) }
    var days by remember(reservation.id) { mutableStateOf(reservation.totalDays.toString()) }; var options by remember(reservation.id) { mutableStateOf(reservation.options) }
    var optionsCost by remember(reservation.id) { mutableStateOf(reservation.optionsCost.toString()) }
    FormDialog("Modifier la réservation", onDismiss, enabled = selectedCar != null && selectedClient != null && days.toIntOrNull()?.let { it > 0 } == true && optionsCost.toDoubleOrNull()?.let { it >= 0 } == true, confirmLabel = "Enregistrer", content = {
        ExposedDropdownMenuBox(expanded = carExpanded, onExpandedChange = { carExpanded = !carExpanded }) {
            OutlinedTextField(selectedCar?.let { "${it.brand} ${it.model}" } ?: "Aucun véhicule", {}, Modifier.menuAnchor().fillMaxWidth(), readOnly = true, label = { Text("Véhicule") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
            ExposedDropdownMenu(expanded = carExpanded, onDismissRequest = { carExpanded = false }) { selectableCars.forEach { car -> DropdownMenuItem(text = { Text("${car.brand} ${car.model} • ${car.dailyRate.toInt()} MAD/j") }, onClick = { selectedCar = car; carExpanded = false }) } }
        }
        ExposedDropdownMenuBox(expanded = clientExpanded, onExpandedChange = { clientExpanded = !clientExpanded }) {
            OutlinedTextField(selectedClient?.fullName ?: "Aucun client", {}, Modifier.menuAnchor().fillMaxWidth(), readOnly = true, label = { Text("Client") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
            ExposedDropdownMenu(expanded = clientExpanded, onDismissRequest = { clientExpanded = false }) { clients.forEach { client -> DropdownMenuItem(text = { Text(client.fullName) }, onClick = { selectedClient = client; clientExpanded = false }) } }
        }
        Field("Durée (jours)", days, KeyboardType.Number) { days = it }; Field("Options", options) { options = it }; Field("Coût des options (MAD)", optionsCost, KeyboardType.Decimal) { optionsCost = it }
        Text("La date de départ reste le ${dateLabel(reservation.startDate)}. Les montants sont recalculés selon le véhicule sélectionné.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }, onSubmit = { onConfirm(selectedCar!!.id, selectedClient!!.id, reservation.startDate, days.toIntOrNull() ?: 1, options, optionsCost.toDoubleOrNull() ?: 0.0) })
}

@Composable
fun AddMaintenanceDialog(car: Car, onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) = MaintenanceDialog(listOf(car), onDismiss) { _, description, cost -> onConfirm(description, cost) }

@Composable
fun AddMaintenanceDialog(cars: List<Car>, onDismiss: () -> Unit, onConfirm: (Int, String, Double) -> Unit) = MaintenanceDialog(cars, onDismiss, onConfirm)

@Composable
private fun MaintenanceDialog(cars: List<Car>, onDismiss: () -> Unit, onConfirm: (Int, String, Double) -> Unit) {
    var selectedCar by remember { mutableStateOf(cars.firstOrNull()) }; var description by remember { mutableStateOf("") }; var cost by remember { mutableStateOf("0") }
    FormDialog("Enregistrer un entretien", onDismiss, enabled = selectedCar != null && description.isNotBlank(), confirmLabel = "Enregistrer", content = {
        if (cars.size > 1) ChoiceRow("Véhicule", cars.map { "${it.brand} ${it.model}" }, selectedCar?.let { "${it.brand} ${it.model}" } ?: "") { value -> selectedCar = cars[cars.indexOfFirst { "${it.brand} ${it.model}" == value }] }
        Field("Description", description) { description = it }; Field("Coût (MAD)", cost, KeyboardType.Decimal) { cost = it }
    }, onSubmit = { onConfirm(selectedCar!!.id, description, cost.toDoubleOrNull() ?: 0.0) })
}

@Composable
fun AddExpenseDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double) -> Unit) {
    var category by remember { mutableStateOf("Autre") }; var description by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("0") }
    FormDialog("Ajouter une dépense", onDismiss, enabled = description.isNotBlank() && amount.toDoubleOrNull()?.let { it > 0 } == true, confirmLabel = "Enregistrer", content = {
        ChoiceRow("Catégorie", listOf("Assurance", "Carburant", "Salaires", "Entretien", "Autre"), category) { category = it }
        Field("Description", description) { description = it }; Field("Montant (MAD)", amount, KeyboardType.Decimal) { amount = it }
    }, onSubmit = { onConfirm(category, description, amount.toDoubleOrNull() ?: 0.0) })
}

@Composable
private fun FormDialog(title: String, onDismiss: () -> Unit, enabled: Boolean, confirmLabel: String, content: @Composable ColumnScope.() -> Unit, onSubmit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } },
        confirmButton = { Button(onClick = onSubmit, enabled = enabled, shape = RoundedCornerShape(12.dp)) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun Field(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType))
}

@Composable
private fun ChoiceRow(label: String, values: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) { Text("$label : $selected", Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null) }
        DropdownMenu(expanded, { expanded = false }) { values.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { onSelected(value); expanded = false }) } }
    }
}

private fun dateLabel(value: Long) = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(java.util.Date(value))

@Composable
private fun DocumentPickerField(label: String, selectedUri: String, onSelected: (String) -> Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        onSelected(uri.toString())
    }
    OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf", "image/*")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Icon(Icons.Default.AttachFile, null, Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (selectedUri.isBlank()) "Joindre $label" else "$label joint", maxLines = 1)
    }
}
