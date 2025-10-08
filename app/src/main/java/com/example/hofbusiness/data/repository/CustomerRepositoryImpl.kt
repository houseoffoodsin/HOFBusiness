package com.example.hofbusiness.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.example.hofbusiness.data.model.Customer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CustomerRepository {

    private val customersCollection = firestore.collection("customers")

    override suspend fun createOrUpdateCustomer(customer: Customer): Result<Unit> {
        return try {
            val customerId = customer.id.ifEmpty { customer.mobileNumber }
            val customerWithId = customer.copy(id = customerId)
            customersCollection.document(customerId).set(customerWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCustomerByPhone(phoneNumber: String): Result<Customer?> {
        return try {
            val document = customersCollection.document(phoneNumber).get().await()
            val customer = document.toObject(Customer::class.java)
            Result.success(customer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun searchCustomersByName(query: String): Flow<List<Customer>> = callbackFlow {
        val listener = customersCollection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + '\uf8ff')
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val customers = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Customer::class.java)
                } ?: emptyList()

                trySend(customers)
            }

        awaitClose { listener.remove() }
    }

    override fun getAllCustomers(): Flow<List<Customer>> = callbackFlow {
        val listener = customersCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val customers = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Customer::class.java)
                } ?: emptyList()

                trySend(customers)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateCustomerOrderCount(customerId: String): Result<Unit> {
        return try {
            val customerDoc = customersCollection.document(customerId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(customerDoc)
                val customer = snapshot.toObject(Customer::class.java)
                if (customer != null) {
                    val updatedCustomer = customer.copy(
                        totalOrders = customer.totalOrders + 1,
                        lastOrderDate = Date()
                    )
                    transaction.set(customerDoc, updatedCustomer)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}