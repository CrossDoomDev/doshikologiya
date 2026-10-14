package ru.doshikologiya.nativepreview

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI

/** Only these two public settings may override the offline configuration. */
internal data class ServiceContacts(val email: String?, val donation: String?) {
    fun applyTo(content: ExtraCatalog) = content.copy(
        supportEmail = email ?: content.supportEmail,
        donateUrl = donation ?: content.donateUrl
    )

    companion object {
        fun parse(json: String): ServiceContacts {
            val value = JSONObject(json)
            fun optionalString(key: String): String? =
                if (!value.has(key)) null else (value.get(key) as? String
                    ?: error("Некорректные контакты"))
            val email = optionalString("supportEmail")?.trim()
            val donation = optionalString("donateUrl")
            require(email != null || donation != null)
            require(email == null || email.isEmpty() || validEmail(email))
            require(donation == null || donation.isEmpty() || (donation.length <= 2048 &&
                runCatching { URI(donation).let {
                    it.scheme == "https" && !it.host.isNullOrBlank() && it.rawUserInfo == null
                } }.getOrDefault(false)))
            return ServiceContacts(email, donation)
        }

        fun validEmail(email: String) = email.length <= 254 &&
            email.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))

        fun cached(context: Context, content: ExtraCatalog): ExtraCatalog = runCatching {
            val json = context.getSharedPreferences("service_contacts", 0)
                .getString("json", null) ?: return content
            parse(json).applyTo(content)
        }.getOrDefault(content)

        suspend fun refresh(context: Context, content: ExtraCatalog): ExtraCatalog =
            withContext(Dispatchers.IO) {
                val json = PublicHttp.bytes(content.remoteConfigUrl, 100_000, 6000)
                    .toString(Charsets.UTF_8)
                val contacts = parse(json)
                context.getSharedPreferences("service_contacts", 0).edit()
                    .putString("json", json).apply()
                contacts.applyTo(content)
            }
    }
}
