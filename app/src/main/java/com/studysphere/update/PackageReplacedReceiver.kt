package com.studysphere.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studysphere.MainActivity

/**
 * Receives [Intent.ACTION_MY_PACKAGE_REPLACED] immediately after Android's
 * package installer finishes replacing this app with a newer APK.
 *
 * Behaviour: relaunches [MainActivity] as a fresh task so the user never has
 * to tap the launcher icon manually after an in-app update.
 *
 * Why MY_PACKAGE_REPLACED (not PACKAGE_REPLACED)?
 *   • MY_PACKAGE_REPLACED is only sent to the updated package itself — no
 *     permission needed and android:exported="false" is correct.
 *   • PACKAGE_REPLACED is sent to all packages and would require
 *     android:exported="true" + a data-scheme filter, which is unnecessary here.
 */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val launch = Intent(context, MainActivity::class.java).apply {
            // Clear any existing task stack so the user lands on a clean start.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(launch)
    }
}
