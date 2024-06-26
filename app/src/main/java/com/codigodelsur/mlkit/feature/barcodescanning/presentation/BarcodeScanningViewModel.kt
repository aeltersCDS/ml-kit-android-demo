package com.codigodelsur.mlkit.feature.barcodescanning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeScanningViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(BarcodeScanningUiState())
    val state: StateFlow<BarcodeScanningUiState> = _state.asStateFlow()

    private val _effects: Channel<BarcodeScanningEffect> = Channel()
    val effects = _effects.receiveAsFlow()

    fun processBarcodes(barcodes: List<Barcode>) {
        viewModelScope.launch {
            if (state.value.isEnabled && !state.value.isLoading && barcodes.isNotEmpty()) {
                // Only process the first barcode
                val barcode = barcodes[0]
                barcode.url?.url?.let { url ->
                    // Simulate a loading
                    _state.update { it.copy(isLoading = true, isEnabled = false) }
                    delay(2000L)
                    _effects.send(BarcodeScanningEffect.OpenWebsite(url = url))
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun enableScanner() {
        _state.update { it.copy(isEnabled = true) }
    }
}