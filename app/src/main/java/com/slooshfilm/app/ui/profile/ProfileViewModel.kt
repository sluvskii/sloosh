package com.slooshfilm.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slooshfilm.app.data.repository.HdRezkaRepository
import com.slooshfilm.app.data.hdrezka.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ProfileViewModel: вычисляет статический флаг залогинен ли пользователь в UI
 * — только если репозиторий сообщает о наличии cookies _и_ в prefs есть сохранённый username.
 */

class ProfileViewModel(private val repository: HdRezkaRepository) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    init {
        // Подписываемся на поток repository.isLoggedIn и решаем показывать ли UI как залогиненный.
        repository.isLoggedIn.onEach { repoFlag ->
            if (repoFlag) {
                // Если репозиторий говорит, что есть cookie, проверим сначала сохранённый username,
                // затем — in-memory UserData (который может быть заполнен фоновым fetch).
                val saved = try { repository.getSavedUserName() } catch (e: Exception) { null }
                val mem = UserData.userName

                when {
                    !saved.isNullOrBlank() -> {
                        _username.value = saved
                        _isLoggedIn.value = true
                    }
                    !mem.isNullOrBlank() -> {
                        // Показать имя, если оно есть в памяти (полезно после background fetch).
                        _username.value = mem
                        _isLoggedIn.value = true
                    }
                    else -> {
                        // Нет имени — не показываем залогиненный UI
                        _username.value = ""
                        _isLoggedIn.value = false
                    }
                }
            } else {
                _username.value = ""
                _isLoggedIn.value = false
            }
        }.launchIn(viewModelScope)
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.login(username, password)
            if (success) {
                // Попытка прочитать и отобразить имя, которое должно сохраниться в LocalStorage
                val saved = try { repository.getSavedUserName() } catch (e: Exception) { null }
                if (!saved.isNullOrBlank()) _username.value = saved
                _isLoggedIn.value = true
                _snackbarMessage.value = "Авторизация прошла успешно"
            } else {
                _error.value = "Неверный логин или пароль"
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun onSnackbarShown() {
        _snackbarMessage.value = null
    }
}