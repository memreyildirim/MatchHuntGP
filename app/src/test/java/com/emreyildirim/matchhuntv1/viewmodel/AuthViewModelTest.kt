package com.emreyildirim.matchhuntv1.viewmodel

import com.emreyildirim.matchhuntv1.data.repository.UserRepository
import com.emreyildirim.matchhuntv1.helpers.MainDispatcherSupport
import com.emreyildirim.matchhuntv1.ui.viewmodel.AuthViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
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
 * AuthViewModel icin temel davranis testleri. Firebase static getInstance cagrilarini
 * mockkStatic ile mockluyor; gercek bir Firebase init'e ihtiyac duymuyor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val mainDispatcher = MainDispatcherSupport()
    private lateinit var firebaseAuth: FirebaseAuth

    @BeforeMethod
    fun setUp() {
        mainDispatcher.setUp()

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk(relaxed = true)
        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns null

        // UserRepository'nin Firestore/Storage/Auth alanlarinin init'te calismamasi icin
        // constructor mock'u kullaniyoruz. ViewModel'in icindeki userRepository tamamen mocklanir.
        mockkConstructor(UserRepository::class)
        coEvery { anyConstructed<UserRepository>().isProfileComplete(any()) } returns true
    }

    @AfterMethod
    fun tearDown() {
        unmockkAll()
        mainDispatcher.tearDown()
    }

    @Test
    fun initialStatesAreEmpty() = runTest {
        val vm = AuthViewModel()

        assertEquals(vm.currentUser.value, null)
        assertEquals(vm.isLoading.value, false)
        assertEquals(vm.error.value, null)
        assertEquals(vm.isEmailVerified.value, false)
        assertEquals(vm.isProfileComplete.value, null)
    }

    @Test
    fun signInWithFirebaseExceptionShouldSetError() = runTest {
        val failedTask: Task<AuthResult> = Tasks.forException(RuntimeException("invalid creds"))
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns failedTask

        val vm = AuthViewModel()
        vm.signIn("a@b.com", "wrong")
        advanceUntilIdle()

        assertNotNull(vm.error.value, "Firebase exception sonrasi error state set edilmeli")
        assertEquals(vm.currentUser.value, null)
        assertEquals(vm.isLoading.value, false)
    }

    @Test
    fun signInWithUnverifiedEmailShouldSignOut() = runTest {
        val user = mockk<FirebaseUser>(relaxed = true)
        every { user.uid } returns "uid-1"
        every { user.isEmailVerified } returns false
        every { user.reload() } returns Tasks.forResult(null)

        val authResult = mockk<AuthResult>(relaxed = true)
        every { authResult.user } returns user
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
        every { firebaseAuth.currentUser } returns user

        val vm = AuthViewModel()
        vm.signIn("a@b.com", "pwd")
        advanceUntilIdle()

        assertEquals(
            vm.error.value,
            "Lütfen e-posta adresinizi doğrulayın",
            "Email dogrulanmamis kullanici icin error mesaji set edilmeli"
        )
    }
}
