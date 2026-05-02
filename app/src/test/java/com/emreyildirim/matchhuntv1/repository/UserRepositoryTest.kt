package com.emreyildirim.matchhuntv1.repository

import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
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

/**
 * UserRepository icin temel davranis testleri. Firestore/Storage statik
 * cagrilarini mockluyor; gercek bir Firebase instance gerekmiyor.
 */
class UserRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var usersCollection: CollectionReference
    private lateinit var eventsCollection: CollectionReference
    private lateinit var profileImagesRef: StorageReference

    @BeforeMethod
    fun setUp() {
        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseStorage::class)

        firestore = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        usersCollection = mockk(relaxed = true)
        eventsCollection = mockk(relaxed = true)
        profileImagesRef = mockk(relaxed = true)

        every { FirebaseFirestore.getInstance() } returns firestore
        every { FirebaseAuth.getInstance() } returns auth
        every { FirebaseStorage.getInstance() } returns storage
        every { firestore.collection("users") } returns usersCollection
        every { firestore.collection("events") } returns eventsCollection

        val rootRef = mockk<StorageReference>(relaxed = true)
        every { storage.reference } returns rootRef
        every { rootRef.child("profile_images") } returns profileImagesRef
    }

    @AfterMethod
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun isProfileCompleteShouldReturnNullWhenDocMissing() = runTest {
        val userDocRef = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { usersCollection.document("u1") } returns userDocRef
        every { userDocRef.get() } returns com.google.android.gms.tasks.Tasks.forResult(snapshot)
        every { snapshot.exists() } returns false

        val repo = UserRepository()
        val result = repo.isProfileComplete("u1")
        assertNull(result, "Doc yoksa null donmeli")
    }

    @Test
    fun isProfileCompleteShouldReturnTrueWhenFieldIsTrue() = runTest {
        val userDocRef = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { usersCollection.document("u1") } returns userDocRef
        every { userDocRef.get() } returns com.google.android.gms.tasks.Tasks.forResult(snapshot)
        every { snapshot.exists() } returns true
        every { snapshot.data } returns mapOf("isProfileComplete" to true)

        val repo = UserRepository()
        assertEquals(repo.isProfileComplete("u1"), true)
    }

    @Test
    fun getUserProfileDataShouldReturnNullOnException() = runTest {
        val userDocRef = mockk<DocumentReference>(relaxed = true)
        every { usersCollection.document("u1") } returns userDocRef
        every { userDocRef.get() } returns com.google.android.gms.tasks.Tasks.forException(
            RuntimeException("offline")
        )

        val repo = UserRepository()
        assertNull(repo.getUserProfileData("u1"))
    }
}
