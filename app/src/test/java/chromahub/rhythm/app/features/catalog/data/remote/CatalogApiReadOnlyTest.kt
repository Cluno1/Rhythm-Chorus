package chromahub.rhythm.app.features.catalog.data.remote

import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

class CatalogApiReadOnlyTest {
    @Test
    fun secondPhaseApiDeclaresOnlyGetAndHead() {
        CatalogApi::class.java.declaredMethods.filterNot { it.name.endsWith("\$default") }.forEach { method ->
            val annotations = method.annotations.map { it.annotationClass.java }
            assertTrue("${method.name} must be GET or HEAD", annotations.any { it == GET::class.java || it == HEAD::class.java })
            assertTrue(
                "${method.name} must not declare a write verb",
                annotations.none { it == POST::class.java || it == PUT::class.java || it == PATCH::class.java || it == DELETE::class.java },
            )
        }
    }
}
