package com.bez.spinwheel_sdk.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.domain.model.WheelResult
import com.bez.spinwheel_sdk.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class SpinViewModel(private val repo: ConfigRepository) : ViewModel() {

    val config: StateFlow<WheelResult<WheelConfig>> = repo.observeConfig()
        .stateIn(viewModelScope, SharingStarted.Eagerly, WheelResult.Loading)

    fun refresh() {
        viewModelScope.launch { repo.fetchConfig() }
    }

    class Factory(private val repo: ConfigRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SpinViewModel(repo) as T
    }
}
