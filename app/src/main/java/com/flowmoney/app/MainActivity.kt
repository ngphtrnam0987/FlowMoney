package com.flowmoney.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.BudgetRepository
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.DailyReviewRepository
import com.flowmoney.app.data.repository.SettingsRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.domain.engine.CategoryClassifier
import com.flowmoney.app.ui.navigation.Screen
import com.flowmoney.app.ui.screens.accounts.AccountManagementScreen
import com.flowmoney.app.ui.screens.analytics.AnalyticsScreen
import com.flowmoney.app.ui.screens.analytics.AnalyticsViewModel
import com.flowmoney.app.ui.screens.budget.BudgetManagementScreen
import com.flowmoney.app.ui.screens.category.CategoryManagementScreen
import com.flowmoney.app.ui.screens.dailyreview.DailyReviewScreen
import com.flowmoney.app.ui.screens.home.HomeScreen
import com.flowmoney.app.ui.screens.home.HomeViewModel
import com.flowmoney.app.ui.screens.onboarding.OnboardingScreen
import com.flowmoney.app.ui.screens.scan.ScanScreen
import com.flowmoney.app.ui.screens.scan.ScanViewModel
import com.flowmoney.app.ui.screens.settings.DiagnosticsScreen
import com.flowmoney.app.ui.screens.settings.DiagnosticsViewModel
import com.flowmoney.app.ui.screens.settings.PrivacyPolicyScreen
import com.flowmoney.app.ui.screens.settings.SettingsScreen
import com.flowmoney.app.ui.screens.settings.SettingsViewModel
import com.flowmoney.app.ui.screens.transactions.AddTransactionScreen
import com.flowmoney.app.ui.screens.transactions.TransactionDetailScreen
import com.flowmoney.app.ui.screens.transactions.TransactionsScreen
import com.flowmoney.app.ui.screens.transactions.TransactionsViewModel
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkBorder
import com.flowmoney.app.ui.theme.FlowDarkSurface
import com.flowmoney.app.ui.theme.FlowMoneyTheme
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var budgetRepository: BudgetRepository

    @Inject
    lateinit var dailyReviewRepository: DailyReviewRepository

    @Inject
    lateinit var categoryClassifier: CategoryClassifier

    private val homeViewModel: HomeViewModel by viewModels()
    private val transactionsViewModel: TransactionsViewModel by viewModels()
    private val scanViewModel: ScanViewModel by viewModels()
    private val analyticsViewModel: AnalyticsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle shared image (e.g. receipt shared from Gallery or other apps)
        handleSharedIntent(intent)

        setContent {
            FlowMoneyTheme {
                MainAppContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (imageUri != null) {
                scanViewModel.processImageUri(this, imageUri)
            }
        }
    }

    @Composable
    fun MainAppContent() {
        val navController = rememberNavController()
        val isOnboardingCompleted by settingsRepository.isOnboardingCompletedFlow.collectAsState(initial = true)
        val isBiometricEnabled by settingsRepository.isBiometricEnabledFlow.collectAsState(initial = false)
        var isAuthenticated by remember { mutableStateOf(!isBiometricEnabled) }

        // Trigger biometric prompt if enabled and not yet authenticated
        LaunchedEffect(isBiometricEnabled) {
            if (isBiometricEnabled && !isAuthenticated) {
                showBiometricPrompt { success ->
                    isAuthenticated = success
                }
            } else {
                isAuthenticated = true
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val bottomTabs = listOf(
            Screen.Home,
            Screen.Transactions,
            Screen.Scan,
            Screen.Analytics,
            Screen.Settings
        )

        val showBottomBar = bottomTabs.any { it.route == currentRoute }

        val startDest = if (!isOnboardingCompleted) Screen.Onboarding.route else Screen.Home.route

        // Handle notification deep links (e.g. daily review reminder)
        LaunchedEffect(Unit) {
            val navigateTo = intent.getStringExtra("navigate_to")
            if (navigateTo == "daily_review") {
                navController.navigate(Screen.DailyReview.route)
            }
        }

        Scaffold(
            containerColor = FlowDarkBackground,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = FlowDarkSurface,
                        tonalElevation = 0.dp
                    ) {
                        bottomTabs.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    screen.icon?.let { icon ->
                                        Icon(imageVector = icon, contentDescription = screen.title)
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FlowPrimary,
                                    selectedTextColor = FlowPrimary,
                                    unselectedIconColor = FlowTextSecondary,
                                    unselectedTextColor = FlowTextSecondary,
                                    indicatorColor = FlowDarkBorder.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDest,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        accountRepository = accountRepository,
                        settingsRepository = settingsRepository,
                        onComplete = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                        onNavigateToDailyReview = { navController.navigate(Screen.DailyReview.route) },
                        onNavigateToAccounts = { navController.navigate(Screen.AccountManagement.route) },
                        onNavigateToTransactionDetail = { id ->
                            navController.navigate(Screen.TransactionDetail.createRoute(id))
                        },
                        onNavigateToAllTransactions = { navController.navigate(Screen.Transactions.route) }
                    )
                }

                composable(Screen.Transactions.route) {
                    TransactionsScreen(
                        viewModel = transactionsViewModel,
                        onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                        onNavigateToTransactionDetail = { id ->
                            navController.navigate(Screen.TransactionDetail.createRoute(id))
                        }
                    )
                }

                composable(Screen.Scan.route) {
                    ScanScreen(
                        viewModel = scanViewModel,
                        onScanSuccess = {
                            navController.navigate(Screen.Transactions.route)
                        }
                    )
                }

                composable(Screen.Analytics.route) {
                    AnalyticsScreen(viewModel = analyticsViewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToAccounts = { navController.navigate(Screen.AccountManagement.route) },
                        onNavigateToBudgets = { navController.navigate(Screen.Budget.route) },
                        onNavigateToCategories = { navController.navigate(Screen.CategoryManagement.route) },
                        onNavigateToPrivacy = { navController.navigate(Screen.PrivacySecurity.route) },
                        onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
                    )
                }

                composable(Screen.Diagnostics.route) {
                    DiagnosticsScreen(
                        viewModel = diagnosticsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AddTransaction.route) {
                    AddTransactionScreen(
                        transactionRepository = transactionRepository,
                        accountRepository = accountRepository,
                        categoryRepository = categoryRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.TransactionDetail.route,
                    arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val txId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                    TransactionDetailScreen(
                        transactionId = txId,
                        transactionRepository = transactionRepository,
                        accountRepository = accountRepository,
                        categoryRepository = categoryRepository,
                        categoryClassifier = categoryClassifier,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.DailyReview.route) {
                    DailyReviewScreen(
                        dailyReviewRepository = dailyReviewRepository,
                        transactionRepository = transactionRepository,
                        categoryRepository = categoryRepository,
                        categoryClassifier = categoryClassifier,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Budget.route) {
                    BudgetManagementScreen(
                        budgetRepository = budgetRepository,
                        categoryRepository = categoryRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.CategoryManagement.route) {
                    CategoryManagementScreen(
                        categoryRepository = categoryRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AccountManagement.route) {
                    AccountManagementScreen(
                        accountRepository = accountRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.PrivacySecurity.route) {
                    PrivacyPolicyScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    private fun showBiometricPrompt(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onResult(false)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(false)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực FlowMoney")
            .setSubtitle("Xác thực vân tay hoặc khuôn mặt để mở ứng dụng")
            .setNegativeButtonText("Hủy")
            .build()

        prompt.authenticate(promptInfo)
    }
}
