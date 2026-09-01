package ma.locaauto.offline.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.locaauto.offline.RentalViewModel
import ma.locaauto.offline.data.*
import ma.locaauto.offline.util.PdfExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Section(val label: String, val shortLabel: String) {
    DASHBOARD("Tableau de bord", "Accueil"),
    FLEET("Flotte", "Flotte"),
    RESERVATIONS("Réservations", "Réserv."),
    CLIENTS("Clients", "Clients"),
    DOCUMENTS("Documents", "Docs"),
    OPERATIONS("Opérations", "Plus")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: RentalViewModel) {
    var section by remember { mutableStateOf(Section.DASHBOARD) }
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val reservations by viewModel.reservations.collectAsStateWithLifecycle()
    val contracts by viewModel.contracts.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val maintenance by viewModel.maintenance.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val revenue by viewModel.monthlyRevenue.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("LocaAuto", fontWeight = FontWeight.ExtraBold)
                        Text("Gestion locale • Maroc", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier.padding(start = 12.dp).size(40.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.DirectionsCar, null, tint = Color.White) }
                },
                actions = {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, "Hors ligne", Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(5.dp))
                            Text("100% local", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Section.values().forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = { section = item },
                        icon = { Icon(item.icon(), contentDescription = item.label) },
                        label = { Text(item.shortLabel, fontSize = 10.sp) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (section) {
                Section.DASHBOARD -> DashboardScreen(cars, clients, reservations, invoices, revenue, onFleet = { section = Section.FLEET }, onReservations = { section = Section.RESERVATIONS })
                Section.FLEET -> FleetScreen(cars, viewModel)
                Section.RESERVATIONS -> ReservationsScreen(reservations, cars, clients, viewModel)
                Section.CLIENTS -> ClientsScreen(clients, viewModel)
                Section.DOCUMENTS -> DocumentsScreen(contracts, invoices, reservations, cars, clients, viewModel)
                Section.OPERATIONS -> OperationsScreen(cars, maintenance, expenses, viewModel)
            }
        }
    }
}

private fun Section.icon() = when (this) {
    Section.DASHBOARD -> Icons.Default.Dashboard
    Section.FLEET -> Icons.Default.DirectionsCar
    Section.RESERVATIONS -> Icons.Default.EventAvailable
    Section.CLIENTS -> Icons.Default.People
    Section.DOCUMENTS -> Icons.Default.Description
    Section.OPERATIONS -> Icons.Default.Build
}

@Composable
private fun DashboardScreen(
    cars: List<Car>, clients: List<Client>, reservations: List<Reservation>, invoices: List<Invoice>, revenue: List<Pair<String, Double>>,
    onFleet: () -> Unit, onReservations: () -> Unit
) {
    val rented = cars.count { it.status == CarStatus.RENTED }
    val available = cars.count { it.status == CarStatus.AVAILABLE }
    val active = reservations.count { it.status == RentalStatus.ACTIVE || it.status == RentalStatus.CONFIRMED }
    val currentMonth = revenue.getOrNull(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH))?.second ?: 0.0
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Bonjour, gestionnaire 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Voici l'état de votre agence aujourd'hui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("CA du mois", "${money(currentMonth)} MAD", Icons.Default.TrendingUp, Modifier.weight(1f))
                MetricCard("Locations actives", "$active", Icons.Default.Key, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Disponibles", "$available", Icons.Default.CheckCircle, Modifier.weight(1f))
                MetricCard("Clients", "${clients.size}", Icons.Default.People, Modifier.weight(1f))
            }
        }
        item { RevenueCard(revenue) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("État de la flotte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onFleet) { Text("Voir tout") }
                    }
                    FleetProgress("Disponible", available, cars.size, Color(0xFF16866F))
                    FleetProgress("En location", rented, cars.size, Color(0xFFE27D32))
                    FleetProgress("Maintenance", cars.count { it.status == CarStatus.MAINTENANCE }, cars.size, Color(0xFFB34D5E))
                }
            }
        }
        item {
            Button(onClick = onReservations, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.AddCircle, null); Spacer(Modifier.width(8.dp)); Text("Créer une réservation")
            }
        }
        item { Text("Dernières réservations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(reservations.take(3), key = { it.id }) { reservation ->
            val client = clients.find { it.id == reservation.clientId }
            val car = cars.find { it.id == reservation.carId }
            SimpleRow(Icons.Default.Event, "${client?.fullName ?: "Client"} • ${car?.brand ?: "Véhicule"} ${car?.model ?: ""}", "${reservation.status} • ${money(reservation.totalPrice)} MAD")
        }
        if (invoices.isEmpty()) item { EmptyState("Aucune facture pour le moment") }
    }
}

@Composable
private fun FleetProgress(label: String, count: Int, total: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 12.sp); Text("$count / $total", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        LinearProgressIndicator(progress = { if (total == 0) 0f else count.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(8.dp), color = color, trackColor = color.copy(alpha = .15f))
    }
}

@Composable
private fun RevenueCard(revenue: List<Pair<String, Double>>) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Revenus encaissés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary)
            }
            val max = revenue.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
            Row(Modifier.fillMaxWidth().height(105.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                revenue.forEach { (month, value) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.fillMaxWidth().height((90 * (value / max)).toFloat().dp.coerceAtLeast(4.dp)).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                        Text(month, fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun FleetScreen(cars: List<Car>, vm: RentalViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var maintenanceCar by remember { mutableStateOf<Car?>(null) }
    var editCar by remember { mutableStateOf<Car?>(null) }
    var deleteCar by remember { mutableStateOf<Car?>(null) }
    ScreenScaffold(title = "Ma flotte", subtitle = "Disponibilité, tarifs et entretien", actionLabel = "Ajouter", onAction = { showAdd = true }) {
        items(cars, key = { it.id }) { car ->
            CarCard(car, onStatus = { vm.updateCarStatus(car.id, it) }, onMaintenance = { maintenanceCar = car }, onEdit = { editCar = car }, onDelete = { deleteCar = car })
        }
        if (cars.isEmpty()) item { EmptyState("Aucun véhicule enregistré") }
    }
    if (showAdd) AddCarDialog(onDismiss = { showAdd = false }) { b, m, c, t, f, r, p, km -> vm.addCar(b, m, c, t, f, r, p, km); showAdd = false }
    maintenanceCar?.let { car -> AddMaintenanceDialog(car, { maintenanceCar = null }) { description, cost -> vm.addMaintenance(car.id, description, cost); maintenanceCar = null } }
    editCar?.let { car -> EditCarDialog(car, { editCar = null }) { updated -> vm.updateCar(updated); editCar = null } }
    deleteCar?.let { car -> DeleteConfirmDialog("Supprimer ce véhicule ?", "Cette action est impossible si le véhicule possède des réservations ou des entretiens.", { deleteCar = null }) { vm.deleteCar(car.id); deleteCar = null } }
}

@Composable
private fun CarCard(car: Car, onStatus: (String) -> Unit, onMaintenance: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.secondary) }
                    Column { Text("${car.brand} ${car.model}", fontWeight = FontWeight.Bold); Text("${car.category} • ${car.year} • ${car.licensePlate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Box {
                    StatusPill(car.status, onClick = { expanded = true })
                    DropdownMenu(expanded, { expanded = false }) {
                        listOf(CarStatus.AVAILABLE, CarStatus.RENTED, CarStatus.MAINTENANCE).forEach { status -> DropdownMenuItem(text = { Text(status) }, onClick = { onStatus(status); expanded = false }) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${car.transmission} • ${car.fuelType} • ${car.mileage} km", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${money(car.dailyRate)} MAD/j", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Modifier") }
                OutlinedButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Delete, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Supprimer") }
            }
            if (car.status != CarStatus.MAINTENANCE) TextButton(onClick = onMaintenance, contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Build, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Planifier un entretien") }
        }
    }
}

@Composable
private fun ReservationsScreen(reservations: List<Reservation>, cars: List<Car>, clients: List<Client>, vm: RentalViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var editReservation by remember { mutableStateOf<Reservation?>(null) }
    var deleteReservation by remember { mutableStateOf<Reservation?>(null) }
    ScreenScaffold(title = "Réservations", subtitle = "Planning et suivi des locations", actionLabel = "Nouvelle", onAction = { showAdd = true }) {
        items(reservations, key = { it.id }) { reservation ->
            val car = cars.find { it.id == reservation.carId }
            val client = clients.find { it.id == reservation.clientId }
            ReservationCard(reservation, car, client, onStatus = { vm.updateReservationStatus(reservation.id, it) }, onEdit = { editReservation = reservation }, onDelete = { deleteReservation = reservation })
        }
        if (reservations.isEmpty()) item { EmptyState("Aucune réservation") }
    }
    if (showAdd) AddReservationDialog(cars, clients, { showAdd = false }) { carId, clientId, days, options, cost -> vm.addReservation(carId, clientId, days, options, cost); showAdd = false }
    editReservation?.let { reservation -> EditReservationDialog(reservation, cars, clients, { editReservation = null }) { carId, clientId, startDate, days, options, cost -> vm.updateReservation(reservation.id, carId, clientId, startDate, days, options, cost); editReservation = null } }
    deleteReservation?.let { reservation -> DeleteConfirmDialog("Supprimer cette réservation ?", "Le contrat et la facture liés seront également supprimés dans la même transaction.", { deleteReservation = null }) { vm.deleteReservation(reservation.id); deleteReservation = null } }
}

@Composable
private fun ReservationCard(reservation: Reservation, car: Car?, client: Client?, onStatus: (String) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("${car?.brand ?: "Véhicule"} ${car?.model ?: "#${reservation.carId}"}", fontWeight = FontWeight.Bold)
                    Text(client?.fullName ?: "Client #${reservation.clientId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    StatusPill(reservation.status, onClick = { expanded = true })
                    DropdownMenu(expanded, { expanded = false }) {
                        listOf(RentalStatus.PENDING, RentalStatus.CONFIRMED, RentalStatus.ACTIVE, RentalStatus.COMPLETED, RentalStatus.CANCELLED).forEach { status -> DropdownMenuItem(text = { Text(status) }, onClick = { onStatus(status); expanded = false }) }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Modifier") }, onClick = { onEdit(); expanded = false })
                        DropdownMenuItem(text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); expanded = false })
                    }
                }
            }
            HorizontalDivider()
            Text("${date(reservation.startDate)} → ${date(reservation.endDate)} • ${reservation.totalDays} jour(s)", fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (reservation.options.isBlank()) "Sans option" else reservation.options, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${money(reservation.totalPrice)} MAD", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            if (reservation.status != RentalStatus.ACTIVE && reservation.status != RentalStatus.COMPLETED) {
                TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Modifier la réservation") }
            }
        }
    }
}

@Composable
private fun ClientsScreen(clients: List<Client>, vm: RentalViewModel) {
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var editClient by remember { mutableStateOf<Client?>(null) }
    var deleteClient by remember { mutableStateOf<Client?>(null) }
    val filtered = clients.filter { it.fullName.contains(query, true) || it.phone.contains(query, true) || it.driverLicenseNumber.contains(query, true) }
    ScreenScaffold(title = "Clients", subtitle = "Fichier des conducteurs", actionLabel = "Ajouter", onAction = { showAdd = true }) {
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Nom, téléphone ou permis") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(14.dp)) }
        items(filtered, key = { it.id }) { client -> ClientCard(client, onEdit = { editClient = client }, onDelete = { deleteClient = client }) }
        if (filtered.isEmpty()) item { EmptyState("Aucun client trouvé") }
    }
    if (showAdd) AddClientDialog({ showAdd = false }) { name, phone, email, license, identity, address -> vm.addClient(name, phone, email, license, identity, address); showAdd = false }
    editClient?.let { client -> EditClientDialog(client, { editClient = null }) { updated -> vm.updateClient(updated); editClient = null } }
    deleteClient?.let { client -> DeleteConfirmDialog("Supprimer ce client ?", "La suppression est refusée si le client possède une réservation.", { deleteClient = null }) { vm.deleteClient(client.id); deleteClient = null } }
}

@Composable
private fun ClientCard(client: Client, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(15.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text(client.fullName.take(1).uppercase(), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.tertiary) }
            Column(Modifier.weight(1f)) {
                Text(client.fullName, fontWeight = FontWeight.Bold)
                Text("${client.phone}${if (client.address.isBlank()) "" else " • ${client.address}"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (client.driverLicenseNumber.isNotBlank()) Text("Permis ${client.driverLicenseNumber}", fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Modifier", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Supprimer", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun DocumentsScreen(contracts: List<Contract>, invoices: List<Invoice>, reservations: List<Reservation>, cars: List<Car>, clients: List<Client>, vm: RentalViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    ScreenScaffold(title = "Documents", subtitle = "Contrats et factures hors ligne") {
        item {
            TabRow(selectedTabIndex = tab) { Tab(tab == 0, { tab = 0 }, text = { Text("Contrats (${contracts.size})") }); Tab(tab == 1, { tab = 1 }, text = { Text("Factures (${invoices.size})") }) }
        }
        if (tab == 0) items(contracts, key = { it.id }) { contract ->
            val res = reservations.find { it.id == contract.reservationId }; val car = cars.find { it.id == res?.carId }; val client = clients.find { it.id == res?.clientId }
            DocumentCard(
                title = "Contrat ${contract.number}", person = client?.fullName ?: "Client",
                vehicle = "${car?.brand ?: "Véhicule"} ${car?.model ?: ""}", status = contract.status,
                amount = "Caution ${money(contract.depositAmount)} MAD",
                onStatus = { vm.updateContractStatus(contract.id, it) },
                onDelete = { vm.deleteContract(contract.id) },
                onExport = { val result = PdfExporter.exportContract(context, contract, client?.fullName ?: "Client", car?.let { c -> "${c.brand} ${c.model}" } ?: "Véhicule"); Toast.makeText(context, result, Toast.LENGTH_LONG).show() }
            )
        } else items(invoices, key = { it.id }) { invoice ->
            val res = reservations.find { it.id == invoice.reservationId }; val car = cars.find { it.id == res?.carId }; val client = clients.find { it.id == res?.clientId }
            DocumentCard(
                title = "Facture ${invoice.number}", person = client?.fullName ?: "Client",
                vehicle = "${car?.brand ?: "Véhicule"} ${car?.model ?: ""}", status = invoice.paymentStatus,
                amount = "Total ${money(invoice.total)} MAD",
                onStatus = { vm.updateInvoiceStatus(invoice.id, it, invoice.paymentMethod) },
                onDelete = { vm.deleteInvoice(invoice.id) },
                onExport = { val result = PdfExporter.exportInvoice(context, invoice, client?.fullName ?: "Client", car?.let { c -> "${c.brand} ${c.model}" } ?: "Véhicule"); Toast.makeText(context, result, Toast.LENGTH_LONG).show() }
            )
        }
        if ((tab == 0 && contracts.isEmpty()) || (tab == 1 && invoices.isEmpty())) item { EmptyState("Aucun document") }
    }
}

@Composable
private fun DocumentCard(title: String, person: String, vehicle: String, status: String, amount: String, onStatus: (String) -> Unit, onDelete: () -> Unit, onExport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text(title, fontWeight = FontWeight.Bold); Text(person, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Box { StatusPill(status, { expanded = true }); DropdownMenu(expanded, { expanded = false }) {
                    val statuses = if (title.startsWith("Contrat")) listOf(ContractStatus.PENDING, ContractStatus.SIGNED, ContractStatus.ACTIVE, ContractStatus.CLOSED, ContractStatus.CANCELLED) else listOf(PaymentStatus.PENDING, PaymentStatus.PAID, PaymentStatus.LATE, PaymentStatus.CANCELLED)
                    statuses.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { onStatus(item); expanded = false }) }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) }, onClick = { confirmDelete = true; expanded = false })
                } }
            }
            Text(vehicle, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(amount, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onExport, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) { Icon(Icons.Default.Download, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("PDF") }
            }
        }
    }
    if (confirmDelete) DeleteConfirmDialog("Supprimer ce document ?", "La suppression respecte les liens avec la réservation et peut être refusée si le document est encore utilisé.", { confirmDelete = false }) { onDelete(); confirmDelete = false }
}

@Composable
private fun OperationsScreen(cars: List<Car>, maintenance: List<MaintenanceRecord>, expenses: List<Expense>, vm: RentalViewModel) {
    var showExpense by remember { mutableStateOf(false) }
    var showMaintenance by remember { mutableStateOf(false) }
    ScreenScaffold(title = "Opérations", subtitle = "Entretien et dépenses de l'agence", actionLabel = "Dépense", onAction = { showExpense = true }) {
        item { Text("Entretiens récents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(maintenance, key = { it.id }) { record ->
            val car = cars.find { it.id == record.carId }
            SimpleRow(Icons.Default.Build, "${car?.brand ?: "Véhicule"} ${car?.model ?: ""}", "${record.description} • ${money(record.cost)} MAD")
        }
        item { OutlinedButton(onClick = { showMaintenance = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Enregistrer un entretien") } }
        item { Text("Dépenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(expenses, key = { it.id }) { expense -> SimpleRow(Icons.Default.ReceiptLong, expense.category, "${expense.description} • ${money(expense.amount)} MAD") }
        if (maintenance.isEmpty() && expenses.isEmpty()) item { EmptyState("Aucune opération enregistrée") }
    }
    if (showExpense) AddExpenseDialog({ showExpense = false }) { category, description, amount -> vm.addExpense(category, description, amount); showExpense = false }
    if (showMaintenance) AddMaintenanceDialog(cars.filter { it.status != CarStatus.MAINTENANCE }, { showMaintenance = false }) { carId, description, cost -> vm.addMaintenance(carId, description, cost); showMaintenance = false }
}

@Composable
private fun ScreenScaffold(title: String, subtitle: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, content: LazyListScope.() -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                if (actionLabel != null && onAction != null) Button(onClick = onAction, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Icon(Icons.Default.Add, null, Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text(actionLabel) }
            }
        }
        content()
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)); Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) } }
}

@Composable
private fun SimpleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(13.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)); Column { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}

@Composable
private fun StatusPill(status: String, onClick: () -> Unit) {
    val color = when (status) { CarStatus.AVAILABLE, RentalStatus.CONFIRMED, ContractStatus.ACTIVE, PaymentStatus.PAID -> Color(0xFF16866F); CarStatus.RENTED, RentalStatus.ACTIVE, ContractStatus.SIGNED -> Color(0xFFE27D32); CarStatus.MAINTENANCE, RentalStatus.CANCELLED, ContractStatus.CANCELLED, PaymentStatus.LATE -> Color(0xFFB34D5E); else -> Color(0xFF6B7280) }
    Surface(onClick = onClick, color = color.copy(alpha = .12f), shape = RoundedCornerShape(20.dp)) { Text(status, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color) }
}

@Composable
private fun EmptyState(text: String) { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Inbox, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.outline); Spacer(Modifier.height(8.dp)); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable
private fun DeleteConfirmDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Supprimer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private fun money(value: Double) = String.format(Locale.FRANCE, "%,.2f", value)
private fun date(value: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(value))
