package com.kilocode.android.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand ───────────────────────────────────────────────────────────────────
val Brand         = Color(0xFF7C6FCD)   // soft indigo — primary accent
val BrandDim      = Color(0xFF5C52A2)   // deeper indigo for pressed states
val BrandGlow     = Color(0xFFA89EE0)   // lighter for tonal surfaces

// ── Neutrals (dark-first; each has a light-mode twin in Theme.kt) ──────────
val Ink900        = Color(0xFF0E0E12)   // near-black background
val Ink800        = Color(0xFF16161C)   // card surfaces
val Ink700        = Color(0xFF1E1E27)   // elevated surfaces
val Ink600        = Color(0xFF28283A)   // borders
val Ink400        = Color(0xFF5C5C7A)   // muted text / disabled
val Ink200        = Color(0xFFB0AECF)   // secondary text
val Ink50         = Color(0xFFE8E7F4)   // primary text on dark

// ── Semantic ────────────────────────────────────────────────────────────────
val SemanticError       = Color(0xFFE05C6A)
val SemanticErrorSurface= Color(0xFF2A1820)
val SemanticSuccess     = Color(0xFF4CC98A)
val SemanticSuccessSurface = Color(0xFF122818)
val SemanticWarning     = Color(0xFFF4A24A)
val SemanticWarningBg   = Color(0xFF231A0E)

// ── Chat bubbles ────────────────────────────────────────────────────────────
val BubbleUser      = Color(0xFF2A2848)   // indigo-tinted user bubble
val BubbleAssistant = Color(0xFF1C1C26)   // dark neutral for assistant

// ── Tool states ─────────────────────────────────────────────────────────────
val ToolRunning   = Color(0xFF1A1E30)
val ToolSuccess   = Color(0xFF122818)
val ToolError     = Color(0xFF2A1820)
