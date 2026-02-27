package com.bez.spinwheel_sdk.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.domain.model.WheelResult
import com.bez.spinwheel_sdk.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SpinViewModel @Inject constructor(private val repo: ConfigRepository) : ViewModel() {

    val config: StateFlow<WheelResult<WheelConfig>> = repo.observeConfig()
        .stateIn(viewModelScope, SharingStarted.Eagerly, WheelResult.Loading)

    fun refresh() {
        viewModelScope.launch { repo.fetchConfig() }
    }
}
