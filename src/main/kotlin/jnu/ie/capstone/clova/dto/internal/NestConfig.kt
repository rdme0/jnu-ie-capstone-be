package jnu.ie.capstone.clova.dto.internal

import jnu.ie.capstone.clova.enums.ClovaSpeechLanguage

data class NestConfigDTO(
    val transcription: TranscriptionConfig
)

data class TranscriptionConfig(
    val language: ClovaSpeechLanguage
)