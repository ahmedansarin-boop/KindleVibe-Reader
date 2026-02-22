package com.kindlevibe.reader.ui.screens

import android.view.MotionEvent
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kindlevibe.reader.reader.LocatorCodec
import com.kindlevibe.reader.reader.PreferencesMapper
import com.kindlevibe.reader.ui.utils.ImmersiveMode
import com.kindlevibe.reader.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val activity = LocalContext.current as FragmentActivity
    val fragmentManager = activity.supportFragmentManager
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val publication by viewModel.publication.collectAsState()
    val book by viewModel.book.collectAsState()
    val openError by viewModel.openError.collectAsState()
    val locator by viewModel.currentLocator.collectAsState()
    val userPrefs by viewModel.userPrefs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val tocLinks = publication?.tableOfContents ?: emptyList()

    var showOverlay by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }

    ImmersiveMode(active = !showOverlay)

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onReaderStop() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            TocDrawer(
                tocLinks = tocLinks,
                onLinkClick = { link ->
                    val navigator = fragmentManager.findFragmentByTag("epub_navigator")
                        as? EpubNavigatorFragment
                    val targetLocator = publication?.locatorFromLink(link)
                    if (navigator != null && targetLocator != null) {
                        navigator.go(targetLocator)
                    }
                },
                onDismiss = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                openError != null -> {
                    ReaderErrorScreen(
                        message = openError!!,
                        onBack = { viewModel.clearError(); onBack() }
                    )
                }

                publication == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Opening book...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                else -> {
                    val statusBarHeight = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding()
                    val topPadding = statusBarHeight + 18.dp

                    val bgColor = when (userPrefs.theme) {
                        "night" -> Color(0xFF111418)
                        "sepia" -> Color(0xFFF5E6C8)
                        "paper" -> Color(0xFFEDE0CC)
                        else    -> Color(0xFFFAF8F5)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor)
                    ) {
                    EpubReaderView(
                        publication = publication!!,
                        initialLocator = viewModel.getResumeLocator(),
                        preferences = PreferencesMapper.toEpubPreferences(userPrefs),
                        onLocatorChanged = { viewModel.onLocatorChanged(it) },
                        onTap = { showOverlay = !showOverlay },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topPadding)
                    )
                    }

                    val progressText = locator?.locations?.progression
                        ?.let { "${(it * 100).toInt()}%" } ?: "-%"
                    val chapterTitle = locator?.title ?: ""

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.0f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (chapterTitle.isNotBlank()) {
                                "$chapterTitle - $progressText"
                            } else {
                                progressText
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    AnimatedVisibility(
                        visible = showOverlay,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        ReaderOverlay(
                            title = book?.title ?: "",
                            currentTheme = userPrefs.theme,
                            onThemeChange = { viewModel.updateTheme(it) },
                            fontSize = userPrefs.fontSize,
                            onFontSizeChange = { viewModel.updateFontSize(it) },
                            lineHeight = userPrefs.lineHeight,
                            onLineHeightChange = { viewModel.updateLineHeight(it) },
                            onSearchClick = { showSearch = true },
                            onBookmarkClick = { viewModel.addBookmark() },
                            onBookmarksListClick = { showBookmarks = true },
                            onTocClick = { scope.launch { drawerState.open() } },
                            onBack = onBack,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }

                    if (showSearch) {
                        ModalBottomSheet(
                            onDismissRequest = {
                                showSearch = false
                                viewModel.clearSearch()
                            }
                        ) {
                            SearchSheet(
                                query = searchQuery,
                                results = searchResults,
                                isSearching = isSearching,
                                onQueryChange = { viewModel.searchInBook(it) },
                                onResultClick = { target ->
                                    val navigator = fragmentManager.findFragmentByTag("epub_navigator")
                                        as? EpubNavigatorFragment
                                    if (navigator != null) {
                                        scope.launch { navigator.go(target) }
                                    }
                                    showSearch = false
                                    viewModel.clearSearch()
                                },
                                onDismiss = {
                                    showSearch = false
                                    viewModel.clearSearch()
                                }
                            )
                        }
                    }

                    if (showBookmarks) {
                        ModalBottomSheet(
                            onDismissRequest = { showBookmarks = false }
                        ) {
                            BookmarksSheet(
                                bookmarks = bookmarks,
                                onItemClick = { bookmark ->
                                    val locatorTarget = LocatorCodec.decode(bookmark.locatorJson)
                                    val navigator = fragmentManager.findFragmentByTag("epub_navigator")
                                        as? EpubNavigatorFragment
                                    if (locatorTarget != null && navigator != null) {
                                        scope.launch { navigator.go(locatorTarget) }
                                    }
                                    showBookmarks = false
                                },
                                onDeleteClick = { viewModel.deleteBookmark(it) },
                                onDismiss = { showBookmarks = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpubReaderView(
    publication: Publication,
    initialLocator: Locator?,
    preferences: EpubPreferences,
    onLocatorChanged: (Locator) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as FragmentActivity
    val fragmentManager = activity.supportFragmentManager
    val containerId = remember { View.generateViewId() }
    val navigatorState = remember { mutableStateOf<EpubNavigatorFragment?>(null) }

    LaunchedEffect(preferences) {
        navigatorState.value?.submitPreferences(preferences)
    }

    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = containerId
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { _ ->
        val existingFragment = fragmentManager.findFragmentByTag("epub_navigator")
        val existingNavigator = existingFragment as? EpubNavigatorFragment

        if (
            existingNavigator != null &&
            existingNavigator.isAdded &&
            !existingNavigator.isDetached &&
            existingNavigator.view != null &&
            existingNavigator.id == containerId
        ) {
            navigatorState.value = existingNavigator
            return@AndroidView
        }

        if (existingFragment != null) {
            fragmentManager.beginTransaction()
                .remove(existingFragment)
                .commitNow()
        }

        val navigatorFactory = EpubNavigatorFactory(
            publication = publication,
            configuration = EpubNavigatorFactory.Configuration(
                defaults = EpubDefaults(
                    scroll = false,
                    pageMargins = 1.0
                )
            )
        )

        fragmentManager.fragmentFactory = navigatorFactory.createFragmentFactory(
            initialLocator = initialLocator,
            initialPreferences = preferences,
            listener = null
        )

        val fragment = fragmentManager.fragmentFactory
            .instantiate(
                ClassLoader.getSystemClassLoader(),
                EpubNavigatorFragment::class.java.name
            ) as EpubNavigatorFragment

        fragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(containerId, fragment, "epub_navigator")
            .commitNow()

        navigatorState.value = fragment

        fragment.currentLocator
            .onEach { location -> onLocatorChanged(location) }
            .launchIn(activity.lifecycleScope)

        try {
            fragment.addInputListener(
                object : InputListener {
                    override fun onTap(event: TapEvent): Boolean {
                        val width = activity.window.decorView.width.toFloat()
                        return when {
                            event.point.x < width * 0.30f -> {
                                fragment.goBackward(animated = true)
                                true
                            }

                            event.point.x > width * 0.70f -> {
                                fragment.goForward(animated = true)
                                true
                            }

                            else -> {
                                onTap()
                                true
                            }
                        }
                    }
                }
            )
        } catch (_: Exception) {
            // Tap zones via overlay toggle only if API unavailable
        }
    }
}

@Composable
fun ReaderErrorScreen(message: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            androidx.compose.material3.Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text("Can't open this book", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            androidx.compose.material3.Button(onClick = onBack) { Text("Go Back") }
        }
    }
}
