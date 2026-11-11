package com.studyflow.ui

import androidx.compose.runtime.Composable

/**
 * Lets each platform adapt its system bars (status bar / navigation bar icons) to the
 * app's own light or dark theme. Android implements it, desktop does nothing.
 */
@Composable
expect fun SystemBarsEffect(darkTheme: Boolean)
