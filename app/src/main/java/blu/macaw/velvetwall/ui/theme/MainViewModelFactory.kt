package blu.macaw.velvetwall.ui.theme

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import blu.macaw.velvetwall.data.CallRepository
import blu.macaw.velvetwall.data.UserSettings
import blu.macaw.velvetwall.MainViewModel

// Factory
class MainViewModelFactory(
    private val application: Application,
    private val repository: CallRepository,
    private val userSettings: UserSettings
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository, userSettings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}