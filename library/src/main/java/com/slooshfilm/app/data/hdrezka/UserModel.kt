package com.slooshfilm.app.data.hdrezka

import android.content.Context
import android.util.ArrayMap
import android.webkit.CookieManager
import com.slooshfilm.app.data.hdrezka.BaseModel
import com.slooshfilm.app.data.hdrezka.CookieStorage
import com.slooshfilm.app.data.hdrezka.UserData
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object UserModel {
    private const val USER_PAGE: String = "/user/"
    private const val LOGIN_AJAX: String = "/ajax/login/"
    private const val REGISTER_AJAX: String = "/engine/ajax/quick_register.php"

    fun getUserAvatarLink(context: Context): String? {
        val userId: String? = CookieStorage.getCookie("https://rezka.fi", "dle_user_id")
        val doc: Document = BaseModel.getJsoup("https://rezka.fi" + USER_PAGE + userId, context)
            .header("Cookie", CookieManager.getInstance().getCookie("https://rezka.fi"))
            .get()
        val str = doc.select("div.b-userprofile__avatar_holder img").attr("src")

        return if (str.contains("//static") && str.contains("http") && str.contains("noavatar")) {
            null
        } else if (!str.contains("//static") && !str.contains("http") && str.contains("noavatar")) {
            null
        } else if (str.contains("//static") && str.contains("http")) {
            null
        } else if (str.contains("http") && str.contains("upload")) {
            str
        } else if (!str.contains("http") && str.contains("upload")) {
            "https://rezka.fi" + str
        } else {
            null
        }
    }

    fun getUserName(context: Context): String? {
        try {
            val userId: String? = CookieStorage.getCookie("https://rezka.fi", "dle_user_id")
            if (userId.isNullOrEmpty()) return null

            val doc: Document = BaseModel.getJsoup("https://rezka.fi" + USER_PAGE + userId, context)
                .header("Cookie", CookieManager.getInstance().getCookie("https://rezka.fi"))
                .get()

            // Try several selectors that may contain the display name
            val selectors = listOf(
                "div.b-userprofile__name",
                "div.b-userprofile__info h1",
                "div.b-userprofile__info .name",
                "h1[itemprop=name]",
                "title"
            )

            for (sel in selectors) {
                val el = doc.selectFirst(sel)
                if (el != null) {
                    val text = el.text().trim()
                    if (text.isNotEmpty()) {
                        // If selector was title, try to clean site suffix
                        if (sel == "title") {
                            return text.split("|").firstOrNull()?.trim()
                        }
                        return text
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return null
    }

    fun login(name: String, password: String, context: Context) {
        val data: ArrayMap<String, String> = ArrayMap()
        data["login_name"] = name
        data["login_password"] = password
        data["login_not_save"] = "0"

        val res: Connection.Response = BaseModel.getJsoup("https://rezka.fi" + LOGIN_AJAX, context)
            .data(data)
            .method(Connection.Method.POST)
            .execute()

        val doc = res.parse()

        if (doc != null) {
            val bodyString: String = doc.select("body").text()
            val jsonObject = JSONObject(bodyString)

            val isSuccess: Boolean = jsonObject.getBoolean("success")
            if (isSuccess) {
                UserData.setCookies(res.cookie("dle_user_id"), name, res.cookie("dle_password"), res.cookie("PHPSESSID"), context, true)
            } else {
                val msg = jsonObject.getString("message")
                throw Exception(msg)
            }
        } else {
            throw HttpStatusException("failed to login", 400, "https://rezka.fi")
        }
    }

    fun register(email: String, username: String, password: String, context: Context) {
        val data: ArrayMap<String, String> = ArrayMap()
        data["data"] = "email=$email&prevent_autofill_name=&name=$username&prevent_autofill_password1=&password1=$password&rules=1&submit_reg=submit_reg&do=register"

        val res: Connection.Response = BaseModel.getJsoup("https://rezka.fi" + REGISTER_AJAX, context)
            .data(data)
            .method(Connection.Method.POST)
            .execute()

        val doc = res.parse()

        val scriptTag = doc.select("script")
        if (scriptTag.size > 0) {
            val scriptValue = scriptTag[0].html()

            if ((scriptValue.contains("location") || scriptValue.isEmpty()) && res.hasCookie("dle_user_id") && res.hasCookie("dle_password")) {
                UserData.setCookies(res.cookie("dle_user_id"), username, res.cookie("dle_password"), null, context, true)
            } else {
                val toParse = scriptValue.replace("\$('#register-popup-errors').html('", "").replace("').show();", "")

                val parsedDoc = Jsoup.parse(toParse)
                val errorsEls = parsedDoc.select("li")
                val errorText = StringBuilder()
                for ((i, errorEl) in errorsEls.withIndex()) {
                    errorText.append(errorEl.text())
                    if (i != errorsEls.size - 1) {
                        errorText.append("\n\n")
                    }
                }
                throw Exception(errorText.toString())
            }
        }
    }
}