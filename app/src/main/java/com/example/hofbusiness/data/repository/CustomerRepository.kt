package com.example.hofbusiness.data.repository

import com.example.hofbusiness.data.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    suspend fun createOrUpdateCustomer(customer: Customer): Result<Unit>
    suspend fun getCustomerByPhone(phoneNumber: String): Result<Customer?>
    fun searchCustomersByName(query: String): Flow<List<Customer>>
    fun getAllCustomers(): Flow<List<Customer>>
    suspend fun updateCustomerOrderCount(customerId: String): Result<Unit>
}