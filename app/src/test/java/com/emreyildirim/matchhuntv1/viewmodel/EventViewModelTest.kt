package com.emreyildirim.matchhuntv1.viewmodel

import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.helpers.MainDispatcherSupport
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * EventViewModel testleri. Firestore/Auth statik instance'larini mockluyor.
 * Init blogunda loadEvents() cagrildigi icin Firestore mocklarinin set edilmesi gerekiyor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModelTest {

    private val mainDispatcher = MainDispatcherSupport()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var eventsCollection: CollectionReference

    @BeforeMethod
    fun setUp() {
        mainDispatcher.setUp()

        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)

        firebaseAuth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        eventsCollection = mockk(relaxed = true)

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("events") } returns eventsCollection
    }

    @AfterMethod
    fun tearDown() {
        unmockkAll()
        mainDispatcher.tearDown()
    }

    @Test
    fun initialEventsListShouldBeEmpty() = runTest {
        val emptyQuery = mockk<Query>(relaxed = true)
        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { eventsCollection.orderBy(any<String>(), any()) } returns emptyQuery
        every { emptyQuery.get() } returns com.google.android.gms.tasks.Tasks.forResult(emptySnapshot)
        every { emptySnapshot.documents } returns emptyList()

        val vm = EventViewModel()
        advanceUntilIdle()

        assertEquals(vm.events.value, emptyList<Event>())
        assertEquals(vm.error.value, null)
        assertEquals(vm.eventCreated.value, false)
    }

    @Test
    fun loadEventsExceptionShouldPopulateError() = runTest {
        val query = mockk<Query>(relaxed = true)
        every { eventsCollection.orderBy(any<String>(), any()) } returns query
        every { query.get() } returns com.google.android.gms.tasks.Tasks.forException(RuntimeException("network down"))

        val vm = EventViewModel()
        advanceUntilIdle()

        assertTrue(
            vm.error.value != null,
            "Firestore hata atinca error state doldurulmali"
        )
        assertEquals(vm.isLoading.value, false)
    }

    @Test
    fun resetEventCreatedShouldFlipFlag() = runTest {
        val query = mockk<Query>(relaxed = true)
        val snap = mockk<QuerySnapshot>(relaxed = true)
        every { eventsCollection.orderBy(any<String>(), any()) } returns query
        every { query.get() } returns com.google.android.gms.tasks.Tasks.forResult(snap)
        every { snap.documents } returns emptyList()

        val vm = EventViewModel()
        advanceUntilIdle()

        vm.resetEventCreated()
        assertEquals(vm.eventCreated.value, false)

        vm.resetError()
        assertEquals(vm.error.value, null)
    }
}
