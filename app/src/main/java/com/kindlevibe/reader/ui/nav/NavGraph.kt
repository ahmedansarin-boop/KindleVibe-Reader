package com.kindlevibe.reader.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kindlevibe.reader.ui.screens.AboutScreen
import com.kindlevibe.reader.ui.screens.LibraryScreen
import com.kindlevibe.reader.ui.screens.PdfReaderScreen
import com.kindlevibe.reader.ui.screens.ReaderScreen
import com.kindlevibe.reader.ui.screens.SettingsScreen
import com.kindlevibe.reader.viewmodel.LibraryViewModel

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Library.route
    ) {
        composable(Routes.Library.route) {
            LibraryScreen(
                onOpenSettings = { navController.navigate(Routes.Settings.route) },
                navController = navController
            )
        }
        composable(
            route = "pdf_reader/{bookId}",
            arguments = listOf(navArgument("bookId") { nullable = false })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            val libraryViewModel: LibraryViewModel = viewModel(
                navController.getBackStackEntry(Routes.Library.route)!!
            )
            val book = libraryViewModel.getBookById(bookId)
            book?.let {
                PdfReaderScreen(book = it, navController = navController)
            }
        }
        composable(
            route = Routes.Reader.route,
            arguments = listOf(navArgument("bookId") { nullable = false })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            ReaderScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Routes.About.route) {
            AboutScreen(navController = navController)
        }
    }
}
