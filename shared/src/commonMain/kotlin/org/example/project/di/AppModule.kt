package org.example.project.di

import org.example.project.data.CatalogRepository
import org.example.project.data.ClientRepository
import org.example.project.data.CompanyRepository
import org.example.project.data.InvoiceRepository
import org.example.project.data.PaymentRepository
import org.example.project.data.SettingsRepository
import org.example.project.data.WeatherService
import org.example.project.domain.usecase.ComputeTotals
import org.koin.dsl.module

/**
 * Application-wide Koin module. The platform supplies an [org.example.project.db.AppDatabase]
 * via a platform module passed to [org.example.project.App].
 */
val appModule = module {
    single { ComputeTotals() }
    single { InvoiceRepository(get()) }
    single { PaymentRepository(get()) }
    single { ClientRepository(get()) }
    single { CatalogRepository(get()) }
    single { SettingsRepository(get()) }
    single { CompanyRepository(get()) }
    single { WeatherService() }
}
