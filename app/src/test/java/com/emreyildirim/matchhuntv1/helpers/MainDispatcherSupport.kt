package com.emreyildirim.matchhuntv1.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * TestNG icin Dispatchers.Main destegi. JUnit Rule kullanmadigimiz icin
 * @BeforeMethod / @AfterMethod icinde manuel olarak cagrilir.
 *
 * Kullanim:
 *   private val mainDispatcher = MainDispatcherSupport()
 *   @BeforeMethod fun setUp() = mainDispatcher.setUp()
 *   @AfterMethod fun tearDown() = mainDispatcher.tearDown()
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherSupport(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) {
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    fun tearDown() {
        Dispatchers.resetMain()
    }
}
