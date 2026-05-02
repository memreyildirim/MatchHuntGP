package com.emreyildirim.matchhuntv1.viewmodel

import com.emreyildirim.matchhuntv1.data.model.Message
import com.emreyildirim.matchhuntv1.helpers.MainDispatcherSupport
import com.emreyildirim.matchhuntv1.ui.viewmodel.MessageViewModel
import com.emreyildirim.matchhuntv1.utils.ChatUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.util.Date

/**
 * MessageViewModel ve onunla iliskili ChatUtils icin temel testler.
 * ChatUtils tamamen pure - Firebase init'e ihtiyac duymaz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageViewModelTest {

    private val mainDispatcher = MainDispatcherSupport()

    @BeforeMethod
    fun setUp() {
        mainDispatcher.setUp()

        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
    }

    @AfterMethod
    fun tearDown() {
        unmockkAll()
        mainDispatcher.tearDown()
    }

    @Test
    fun chatIdShouldBeStableRegardlessOfUserOrder() {
        val a = "abc"
        val b = "xyz"
        assertEquals(ChatUtils.getChatId(a, b), ChatUtils.getChatId(b, a))
        assertEquals(ChatUtils.getChatId(a, b), "abc_xyz")
    }

    @Test
    fun initialStatesShouldBeEmpty() = runTest {
        val vm = MessageViewModel(context = null)
        assertEquals(vm.messages.value, emptyList<Message>())
        assertEquals(vm.conversations.value.size, 0)
        assertEquals(vm.isLoading.value, false)
        assertEquals(vm.totalUnreadCount.value, 0)
    }

    @Test
    fun messageDataClassShouldBeCopySafe() {
        val now = Date()
        val msg = Message(
            id = "m1",
            chatId = "abc_xyz",
            senderId = "abc",
            receiverId = "xyz",
            text = "hi",
            timestamp = now,
            isRead = false
        )
        val read = msg.copy(isRead = true)
        assertEquals(read.id, msg.id)
        assertEquals(read.isRead, true)
        assertEquals(msg.isRead, false, "copy original'i mutate etmemeli")
        assertNotNull(msg.timestamp)
    }
}
