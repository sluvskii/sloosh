package com.slooshfilm.app.ui

import android.content.Context
import com.slooshfilm.app.data.hdrezka.UserModel
import com.slooshfilm.app.data.hdrezka.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserPresenter(private val userView: UserView, private val context: Context) {
    fun getUserAvatar() {
        GlobalScope.launch {
            try {
                UserData.setAvatar(UserModel.getUserAvatarLink(context), context)

                withContext(Dispatchers.Main) {
                    userView.setUserAvatar()
                }
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    fun login(name: String, password: String) {
        GlobalScope.launch {
            try {
                UserModel.login(name, password, context)

                withContext(Dispatchers.Main) {
                    UserData.setLoggedIn(context, true)
                    getUserAvatar()
                    userView.completeAuth()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.message?.let { userView.showError(it) }
                }
            }
        }
    }

    fun register(email: String, username: String, password: String) {
        GlobalScope.launch {
            try {
                UserModel.register(email, username, password, context)

                withContext(Dispatchers.Main) {
                    UserData.setLoggedIn(context, true)
                    getUserAvatar()
                    userView.completeAuth()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.message?.let { userView.showError(it) }
                }
            }
        }
    }

    fun exit() {
        UserData.reset(context)
        userView.setUserAvatar()
    }
}

interface UserView {
    fun setUserAvatar()
    fun completeAuth()
    fun showError(message: String)
}