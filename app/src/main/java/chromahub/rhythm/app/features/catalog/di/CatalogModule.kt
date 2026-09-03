package chromahub.rhythm.app.features.catalog.di

import android.content.Context
import chromahub.rhythm.app.features.catalog.data.CatalogRepositoryImpl
import chromahub.rhythm.app.features.catalog.domain.CatalogRepository

object CatalogModule {
    @Volatile private var repository: CatalogRepository? = null

    fun repository(context: Context): CatalogRepository = repository ?: synchronized(this) {
        repository ?: CatalogRepositoryImpl(context.applicationContext).also { repository = it }
    }
}
