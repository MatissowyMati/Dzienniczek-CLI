package io.github.matissowymati.dzienniczek.cli

import io.github.matissowymati.dzienniczek.api.hebe.ExpiredTokenException
import io.github.matissowymati.dzienniczek.api.hebe.FailedRequestException
import io.github.matissowymati.dzienniczek.api.hebe.MissingUnitSymbolException
import io.github.matissowymati.dzienniczek.api.hebe.UsedTokenException
import io.github.matissowymati.dzienniczek.api.hebe.WrongTokenException
import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {
    @Test
    fun classifiesApiExceptionsAsApiErrors() {
        assertEquals(Exit.API, classify(MissingUnitSymbolException("brak symbolu jednostki")))
    }

    @Test
    fun keepsAuthenticationErrorsMoreSpecific() {
        assertEquals(Exit.AUTH, classify(WrongTokenException("nieprawidłowy token")))
    }

    @Test
    fun classifiesExpiredAndUsedTokensAsAuthenticationErrors() {
        assertEquals(Exit.AUTH, classify(ExpiredTokenException("token wygasł")))
        assertEquals(Exit.AUTH, classify(UsedTokenException("token został już użyty")))
    }

    @Test
    fun classifiesFailedRequestsAsNetworkErrors() {
        assertEquals(Exit.NETWORK, classify(FailedRequestException("żądanie nie powiodło się")))
    }
}
