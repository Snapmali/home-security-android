package com.snapkirin.homesecurity.model.http

object ResponseCodes {
    const val Success = 0

    const val Error = 100
    const val NetworkError = 101

    const val InvalidToken     = 301
    const val LoginAgainNeeded = 302

    const val ParameterError          = 400
    const val InvalidUsername         = 401
    const val InvalidEmail            = 402
    const val InvalidPassword         = 403
    const val InvalidVerificationCode = 404
    const val InvalidIdentifierType   = 405
    const val InvalidScreenName       = 406
    const val ClaimNotMatchId         = 407

    const val InternalError   = 500
    const val DatabaseFailure = 501
    const val EmailFailure    = 502
}