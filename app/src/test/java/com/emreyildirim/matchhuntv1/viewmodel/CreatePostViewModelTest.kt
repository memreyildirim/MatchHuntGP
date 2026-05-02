package com.emreyildirim.matchhuntv1.viewmodel

import android.net.Uri
import com.emreyildirim.matchhuntv1.data.model.Post
import com.emreyildirim.matchhuntv1.data.repository.PostRepository
import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.helpers.MainDispatcherSupport
import com.emreyildirim.matchhuntv1.ui.viewmodel.CreatePostViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * CreatePostViewModel testleri. Repository constructor'larini mocklayarak
 * ViewModel davranisini izole sekilde dogruluyor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreatePostViewModelTest {

    private val mainDispatcher = MainDispatcherSupport()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    @BeforeMethod
    fun setUp() {
        mainDispatcher.setUp()

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk(relaxed = true)
        firebaseUser = mockk(relaxed = true)
        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "user-1"

        mockkConstructor(UserRepository::class)
        mockkConstructor(PostRepository::class)
    }

    @AfterMethod
    fun tearDown() {
        unmockkAll()
        mainDispatcher.tearDown()
    }

    @Test
    fun createPostWithoutLoggedUserShouldSetError() = runTest {
        every { firebaseAuth.currentUser } returns null

        val vm = CreatePostViewModel()
        vm.createPost(mockk<Uri>(relaxed = true), "desc", "football")
        advanceUntilIdle()

        assertNotNull(vm.error.value)
        assertEquals(vm.postCreated.value, false)
    }

    @Test
    fun createPostWithMissingProfileShouldSetError() = runTest {
        coEvery { anyConstructed<UserRepository>().getUserProfileData("user-1") } returns null

        val vm = CreatePostViewModel()
        vm.createPost(mockk<Uri>(relaxed = true), "desc", "football")
        advanceUntilIdle()

        assertEquals(
            vm.error.value,
            "Kullanıcı profili bulunamadı. Lütfen profil bilgilerinizi tamamlayın."
        )
        assertEquals(vm.postCreated.value, false)
    }

    @Test
    fun createPostHappyPathShouldFlipPostCreated() = runTest {
        coEvery { anyConstructed<UserRepository>().getUserProfileData("user-1") } returns
            mapOf("username" to "emre")
        val savedPost = Post(id = "p", userId = "user-1", userName = "emre")
        coEvery {
            anyConstructed<PostRepository>().createPost(
                userId = "user-1",
                userName = "emre",
                imageUri = any(),
                description = any(),
                sportType = any()
            )
        } returns Result.success(savedPost)

        val vm = CreatePostViewModel()
        vm.createPost(mockk<Uri>(relaxed = true), "desc", "football")
        advanceUntilIdle()

        assertEquals(vm.postCreated.value, true)
        assertEquals(vm.error.value, null)
        assertEquals(vm.isLoading.value, false)
    }
}
