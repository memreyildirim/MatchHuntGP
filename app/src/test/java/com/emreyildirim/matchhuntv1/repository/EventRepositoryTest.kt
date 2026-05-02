package com.emreyildirim.matchhuntv1.repository

import com.emreyildirim.matchhuntv1.data.model.Event
import com.emreyildirim.matchhuntv1.data.repository.EventRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNull
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class EventRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var eventsCollection: CollectionReference

    @BeforeMethod
    fun setUp() {
        mockkStatic(FirebaseFirestore::class)
        firestore = mockk(relaxed = true)
        eventsCollection = mockk(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("events") } returns eventsCollection
    }

    @AfterMethod
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getEventByIdShouldReturnNullOnException() = runTest {
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { eventsCollection.document("e1") } returns docRef
        every { docRef.get() } returns com.google.android.gms.tasks.Tasks.forException(
            RuntimeException("offline")
        )

        val repo = EventRepository()
        assertNull(repo.getEventById("e1"))
    }

    @Test
    fun getEventByIdShouldReturnMappedEventOnSuccess() = runTest {
        val event = Event(
            id = "e1",
            title = "Test",
            sportType = "football",
            createdBy = "u1"
        )
        val docRef = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { eventsCollection.document("e1") } returns docRef
        every { docRef.get() } returns com.google.android.gms.tasks.Tasks.forResult(snapshot)
        every { snapshot.toObject(Event::class.java) } returns event

        val repo = EventRepository()
        val result = repo.getEventById("e1")
        assertEquals(result?.id, "e1")
        assertEquals(result?.title, "Test")
    }
}
