package ma.locaauto.offline.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RentalDao {
    @Query("SELECT * FROM cars ORDER BY brand, model")
    fun observeCars(): Flow<List<Car>>

    @Query("SELECT * FROM clients ORDER BY fullName")
    fun observeClients(): Flow<List<Client>>

    @Query("SELECT * FROM reservations ORDER BY createdAt DESC")
    fun observeReservations(): Flow<List<Reservation>>

    @Query("SELECT * FROM contracts ORDER BY signedAt DESC")
    fun observeContracts(): Flow<List<Contract>>

    @Query("SELECT * FROM invoices ORDER BY issuedAt DESC")
    fun observeInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM maintenance_records ORDER BY date DESC")
    fun observeMaintenance(): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun observeExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun getCar(id: Int): Car?

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClient(id: Int): Client?

    @Query("SELECT COUNT(*) FROM cars WHERE licensePlate = :licensePlate AND id != :excludedId")
    suspend fun countCarsWithPlate(licensePlate: String, excludedId: Int): Int

    @Query("SELECT COUNT(*) FROM reservations WHERE carId = :carId")
    suspend fun countReservationsForCar(carId: Int): Int

    @Query("SELECT COUNT(*) FROM reservations WHERE clientId = :clientId")
    suspend fun countReservationsForClient(clientId: Int): Int

    @Query("SELECT COUNT(*) FROM maintenance_records WHERE carId = :carId")
    suspend fun countMaintenanceForCar(carId: Int): Int

    @Query("SELECT COUNT(*) FROM reservations WHERE carId = :carId AND status = 'En cours'")
    suspend fun countActiveReservationsForCar(carId: Int): Int

    @Query("SELECT COUNT(*) FROM reservations WHERE carId = :carId AND status NOT IN ('Annulée', 'Terminée')")
    suspend fun countOpenReservationsForCar(carId: Int): Int

    @Query("SELECT * FROM reservations WHERE id = :id")
    suspend fun getReservation(id: Int): Reservation?

    @Query("SELECT COUNT(*) FROM reservations WHERE carId = :carId AND status NOT IN ('Annulée', 'Terminée') AND startDate < :endDate AND endDate > :startDate")
    suspend fun countOverlappingReservations(carId: Int, startDate: Long, endDate: Long): Int

    @Query("SELECT * FROM contracts WHERE reservationId = :reservationId LIMIT 1")
    suspend fun getContractForReservation(reservationId: Int): Contract?

    @Query("SELECT * FROM contracts WHERE id = :id LIMIT 1")
    suspend fun getContract(id: Int): Contract?

    @Query("SELECT * FROM invoices WHERE contractId = :contractId LIMIT 1")
    suspend fun getInvoiceForContract(contractId: Int): Invoice?

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoice(id: Int): Invoice?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCar(car: Car): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertClient(client: Client): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReservation(reservation: Reservation): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertContract(contract: Contract): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMaintenance(record: MaintenanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateCar(car: Car)

    @Update
    suspend fun updateClient(client: Client)

    @Update
    suspend fun updateReservation(reservation: Reservation)

    @Update
    suspend fun updateContract(contract: Contract)

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Delete
    suspend fun deleteReservation(reservation: Reservation)

    @Delete
    suspend fun deleteCar(car: Car)

    @Delete
    suspend fun deleteClient(client: Client)

    @Delete
    suspend fun deleteContract(contract: Contract)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)
}
