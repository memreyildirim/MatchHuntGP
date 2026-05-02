package com.emreyildirim.matchhuntv1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emreyildirim.matchhuntv1.ui.viewmodel.EventViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import com.emreyildirim.matchhuntv1.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewModel: EventViewModel,
    onNavigateToProfile: (String) -> Unit
) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {

            val findTabCd = stringResource(R.string.cd_tab_find_event)
            val createTabCd = stringResource(R.string.cd_tab_create_event)

            Tab(
                modifier = Modifier.semantics { contentDescription = findTabCd },
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text(stringResource(R.string.tab_find_event_label)) }
            )
            Tab(
                modifier = Modifier.semantics { contentDescription = createTabCd },
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text(stringResource(R.string.tab_create_event_label)) }
            )
        }
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> FindEventScreen(
                    viewModel = viewModel,
                    pagerState = pagerState,
                    onNavigateToProfile = onNavigateToProfile
                )
                1 -> CreateEventScreen(
                    viewModel = viewModel,
                    pagerState = pagerState
                )
            }
        }
    }
} 