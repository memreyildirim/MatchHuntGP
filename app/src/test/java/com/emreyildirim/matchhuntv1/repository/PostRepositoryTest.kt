package com.emreyildirim.matchhuntv1.repository

import com.emreyildirim.matchhuntv1.data.model.Post
import com.emreyildirim.matchhuntv1.data.repository.PostRepository
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
import org.testng.Assert.assertTrue
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class PostRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var postsCollection: CollectionReference

    @BeforeMethod
    fun setUp() {
        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseStorage::class)

        firestore = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        postsCollection = mockk(relaxed = true)

        every { FirebaseFirestore.getInstance() } returns firestore
        every { FirebaseStorage.getInstance() } returns storage
        every { firestore.collection("posts") } returns postsCollection
        every { firestore.collection("comments") } returns mockk(relaxed = true)
        every { storage.reference } returns mockk<StorageReference>(relaxed = true)
    }

    @AfterMethod
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun likePostWhenPostMissingShouldFail() = runTest {
        val docRef = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { postsCollection.document("p1") } returns docRef
        every { docRef.get() } returns com.google.android.gms.tasks.Tasks.forResult(snapshot)
        every { snapshot.toObject(Post::class.java) } returns null

        val repo = PostRepository()
        val result = repo.likePost("p1", "u1")
        assertTrue(result.isFailure, "Post yoksa likePost failure dondurmeli")
    }

    @Test
    fun likePostShouldToggleLikeStateForExistingUser() = runTest {
        val post = Post(
            id = "p1",
            userId = "owner",
            likes = 1,
            likedBy = listOf("u1")
        )
        val docRef = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { postsCollection.document("p1") } returns docRef
        every { docRef.get() } returns com.google.android.gms.tasks.Tasks.forResult(snapshot)
        every { snapshot.toObject(Post::class.java) } returns post
        every { docRef.update(any<Map<String, Any>>()) } returns
            com.google.android.gms.tasks.Tasks.forResult(null)

        val repo = PostRepository()
        val result = repo.likePost("p1", "u1")
        assertTrue(result.isSuccess, "Var olan like'i kaldirmak basarili olmali")
    }

    @Test
    fun likePostShouldAddNewLikerForNonExistingUser() = runTest {
        val post = Post(
            id = "p1",
            userId = "owner",
            likes = 0,
            likedBy = emptyList()
        )
        val docRef = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { postsCollection.document("p1") } returns docRef
        every { docRef.get() } returns com.google.android.gms.tasks.Tasks.forResult(snapshot)
        every { snapshot.toObject(Post::class.java) } returns post
        every { docRef.update(any<Map<String, Any>>()) } returns
            com.google.android.gms.tasks.Tasks.forResult(null)

        val repo = PostRepository()
        val result = repo.likePost("p1", "u-new")
        assertEquals(result.isSuccess, true)
    }
}
