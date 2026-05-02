package com.emreyildirim.matchhuntv1.viewmodel

import com.emreyildirim.matchhuntv1.data.model.Post
import com.emreyildirim.matchhuntv1.data.repository.PostRepository
import com.emreyildirim.matchhuntv1.helpers.MainDispatcherSupport
import com.emreyildirim.matchhuntv1.ui.viewmodel.PostViewModel
import com.emreyildirim.matchhuntv1.ui.viewmodel.SocialFeedViewModel
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
import org.testng.Assert.assertTrue
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.util.Date

/**
 * SocialFeedViewModel testleri. PostRepository constructor'ini mocklayarak gercek
 * Firestore baglantisi olmadan davranis testi yapiyoruz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SocialFeedViewModelTest {

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
        every { firebaseUser.uid } returns "user-self"

        // PostRepository tum cagrilari icin default mock
        mockkConstructor(PostRepository::class)
    }

    @AfterMethod
    fun tearDown() {
        unmockkAll()
        mainDispatcher.tearDown()
    }

    @Test
    fun loadPostsShouldFilterCurrentUserPosts() = runTest {
        val ownPost = Post(id = "p1", userId = "user-self", createdAt = Date())
        val otherPost = Post(id = "p2", userId = "user-other", createdAt = Date())
        coEvery { anyConstructed<PostRepository>().getPostsPaginated(null) } returns
            Result.success(listOf(ownPost, otherPost) to false)

        val postVm = mockk<PostViewModel>(relaxed = true)
        val vm = SocialFeedViewModel(postViewModel = postVm)
        vm.loadPosts()
        advanceUntilIdle()

        // Kendi postu filtreleniyor olmali
        assertEquals(vm.posts.value.map { it.id }, listOf("p2"))
        assertEquals(vm.hasMorePosts.value, false)
    }

    @Test
    fun loadPostsExceptionShouldSetError() = runTest {
        coEvery { anyConstructed<PostRepository>().getPostsPaginated(null) } returns
            Result.failure(RuntimeException("boom"))

        val vm = SocialFeedViewModel(postViewModel = mockk(relaxed = true))
        vm.loadPosts()
        advanceUntilIdle()

        assertTrue(vm.error.value != null, "Repo failure -> error state set olmali")
        assertEquals(vm.posts.value, emptyList<Post>())
    }

    @Test
    fun navigateToProfileShouldExposeAndResetTarget() {
        coEvery { anyConstructed<PostRepository>().getPostsPaginated(any()) } returns
            Result.success(emptyList<Post>() to false)

        val vm = SocialFeedViewModel(postViewModel = mockk(relaxed = true))
        vm.navigateToProfile("u-42")
        assertEquals(vm.navigateToProfile.value, "u-42")
        vm.onProfileNavigationHandled()
        assertEquals(vm.navigateToProfile.value, null)
    }
}
