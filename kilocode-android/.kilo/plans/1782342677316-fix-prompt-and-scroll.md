# Plan to fix PromptInput and SessionScreen

## PromptInput
- Change `BasicTextField` to use `TextFieldValue` instead of `String` to potentially fix the "double typing" issue, as it is more robust for IME interactions.
- Ensure `onValueChange` correctly updates the `TextFieldValue`.

## SessionScreen
- Improve initial scroll-to-bottom behavior.
- Use `LaunchedEffect` to `scrollToItem` (not `animateScrollToItem` to avoid unnecessary animation) when the session is loaded and messages are available for the first time.
