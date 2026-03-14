package com.titanbiosync.ui.session

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.domain.model.SensorReading
import com.titanbiosync.domain.model.Session
import com.titanbiosync.domain.usecase.sensor.GetSessionDataUseCase
import com.titanbiosync.domain.usecase.sensor.RecordSensorReadingUseCase
import com.titanbiosync.domain.usecase.session.EndSessionUseCase
import com.titanbiosync.domain.usecase.session.GetActiveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random
import com.titanbiosync.location.LocationTracker
import com.titanbiosync.domain.usecase.location.RecordLocationUseCase

data class SessionDetailUiState(
    val session: Session? = null,
    val sensorReadings: List<SensorReading> = emptyList(),
    val isLoading: Boolean = true,
    val sessionEnded: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val getSessionDataUseCase: GetSessionDataUseCase,
    private val recordSensorReadingUseCase: RecordSensorReadingUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val locationTracker: LocationTracker,
    private val recordLocationUseCase: RecordLocationUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId")
        ?: throw IllegalArgumentException("sessionId required")

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private var locationTrackingJob: Job? = null
    private var baseHr = 75f

    companion object {
        private const val TAG = "SessionDetailVM"
    }

    init {
        loadSessionData()
        startLocationTracking()
    }

    fun startLocationTracking() {
        if (locationTrackingJob != null) return

        locationTrackingJob = viewModelScope.launch {
            try {
                locationTracker.getLocationUpdates().collect { location ->
                    recordLocationUseCase(
                        RecordLocationUseCase.Params(
                            sessionId = sessionId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            speed = location.speed,
                            bearing = location.bearing,
                            accuracy = location.accuracy,
                            timestamp = location.timestamp
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Location tracking error: ${e.message}")
            }
        }
    }

    fun stopLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            try {
                getSessionDataUseCase(
                    GetSessionDataUseCase.Params(sessionId)
                ).collect { readings ->
                    _uiState.value = _uiState.value.copy(
                        sensorReadings = readings,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun simulateLiveData() {
        simulationJob?.cancel()

        simulationJob = viewModelScope.launch {
            try {
                repeat(60) { i ->
                    if (!isActive) {
                        Log.d("SessionDetailVM", "Simulation stopped")
                        return@launch
                    }

                    val variation = Random.nextFloat() * 10 - 5
                    val trend = kotlin.math.sin(i * 0.1) * 15
                    val hr = (baseHr + trend + variation).coerceIn(60.0, 180.0)

                    recordSensorReadingUseCase(
                        RecordSensorReadingUseCase.Params(
                            deviceId = "SIMULATOR_001",
                            sensorType = "heart_rate",
                            value = hr.toInt().toString(),
                            qualityScore = 0.95f,
                            sessionId = sessionId
                        )
                    )

                    delay(1000)
                }
            } catch (e: CancellationException) {
                Log.d("SessionDetailVM", "Simulation cancelled (normal)")
            } catch (e: Exception) {
                Log.e("SessionDetailVM", "Simulation error: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Simulation error: ${e.message}")
            }
        }
    }

    fun recordHeartRate(hr: Int) {
        viewModelScope.launch {
            try {
                recordSensorReadingUseCase(
                    RecordSensorReadingUseCase.Params(
                        deviceId = "BLE_DEVICE",
                        sensorType = "heart_rate",
                        value = hr.toString(),
                        qualityScore = 0.95f,
                        sessionId = sessionId
                    )
                )
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                Log.e("SessionDetailVM", "Error recording HR: ${e.message}")
            }
        }
    }

    fun endSession() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                simulationJob?.cancel()
                simulationJob = null
                stopLocationTracking()

                endSessionUseCase(
                    EndSessionUseCase.Params(sessionId)
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sessionEnded = true
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun getSessionId(): String = sessionId

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        stopLocationTracking()
    }
}