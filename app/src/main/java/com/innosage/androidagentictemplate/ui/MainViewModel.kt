package com.innosage.androidagentictemplate.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.innosage.androidagentictemplate.AudioRecordService
import com.innosage.androidagentictemplate.data.AppDatabase
import com.innosage.androidagentictemplate.data.UtteranceEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val utteranceDao = database.utteranceDao()

    val utterances: StateFlow<List<UtteranceEntity>> = utteranceDao.getRecentUtterancesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isVoiced: StateFlow<Boolean> = AudioRecordService.isVoicedState
    val currentUtteranceText: StateFlow<String> = AudioRecordService.currentUtteranceText
}
