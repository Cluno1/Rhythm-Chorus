package chromahub.rhythm.app.util

import android.net.Uri
import com.google.gson.*
import java.lang.reflect.Type

/**
 * Provides a single Gson instance configured with custom type adapters that the whole app can reuse.
 * Currently registers an adapter for android.net.Uri so that Uri objects can be safely
 * serialized / deserialized to / from JSON.
 */
object GsonUtils {
    private class UriAdapter : JsonSerializer<Uri>, JsonDeserializer<Uri> {
        override fun serialize(src: Uri?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.toString())
        }

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Uri? {
            if (json == null || json is JsonNull) return null
            return json.asString?.let { Uri.parse(it) }
        }
    }

    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriAdapter())
        .create()
}
