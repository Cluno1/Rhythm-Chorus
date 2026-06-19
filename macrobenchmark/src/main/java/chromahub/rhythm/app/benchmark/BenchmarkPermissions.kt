package chromahub.rhythm.app.benchmark

import android.Manifest
import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope

internal fun MacrobenchmarkScope.grantRhythmRuntimePermissions(packageName: String) {
    val permissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
    }

    permissions.forEach { permission ->
        device.executeShellCommand("pm grant $packageName $permission")
    }
}
