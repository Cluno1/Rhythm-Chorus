package io.github.cluno1.sonorus.features.catalog.di

import android.content.Context
import io.github.cluno1.sonorus.features.catalog.data.CatalogRepositoryImpl
import io.github.cluno1.sonorus.features.catalog.domain.CatalogRepository

object CatalogModule {
    @Volatile private var repository: CatalogRepository? = null

    fun repository(context: Context): CatalogRepository = repository ?: synchronized(this) {
        repository ?: CatalogRepositoryImpl(context.applicationContext).also { repository = it }
    }
}
