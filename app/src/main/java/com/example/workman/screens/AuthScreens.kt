package com.example.workman.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.R
import com.example.workman.ui.theme.GradientEnd
import com.example.workman.ui.theme.GradientStart
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextMuted
import com.example.workman.viewModels.AuthEvent
import com.example.workman.viewModels.AuthState
import com.example.workman.viewModels.AuthViewModel

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

private fun isValidEmail(email: String): Boolean =
    email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

private fun isStrongEnough(password: String): Boolean = password.length >= 6

/** Branded gradient background shared by the auth screens. */
@Composable
private fun AuthBackground(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(GradientStart, GradientEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
private fun BrandHeader(subtitle: String) {
    Image(
        painter = painterResource(id = R.drawable.ic_workman_logo),
        contentDescription = "WorkMan logo",
        modifier = Modifier
            .size(84.dp)
            .background(Color.White, shape = RoundedCornerShape(24.dp))
            .padding(14.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text("WorkMan", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    Spacer(modifier = Modifier.height(6.dp))
    Text(subtitle, fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f))
    Spacer(modifier = Modifier.height(28.dp))
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = PrimaryBlue) },
        trailingIcon = {
            if (isPassword && onTogglePassword != null) {
                val icon =
                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = onTogglePassword) {
                    Icon(icon, contentDescription = "Toggle password", tint = TextMuted)
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        isError = isError,
        supportingText = {
            if (isError && errorText != null) {
                Text(errorText, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color(0xFFDADCE8),
            focusedLabelColor = PrimaryBlue,
            cursorColor = PrimaryBlue
        )
    )
}

@Composable
private fun PrimaryActionButton(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = !loading,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun DividerWithText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Divider(modifier = Modifier.weight(1f), color = Color(0xFFE3E5EF))
        Text("  $text  ", color = TextMuted, fontSize = 12.sp)
        Divider(modifier = Modifier.weight(1f), color = Color(0xFFE3E5EF))
    }
}

@Composable
fun SocialIcon(resId: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .background(Color(0xFFF5F6FB), shape = RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Segmented tab control used at the top of the auth card to switch between the
 * Sign Up and Sign In flows. The [selectedIndex] highlights the active tab
 * (0 = Sign Up, 1 = Sign In) and [onTabSelected] fires only when a *different*
 * tab is tapped so hosts can navigate to the matching screen.
 */
@Composable
private fun AuthTabs(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Sign Up", "Sign In")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F1F7), shape = RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(11.dp)
                    )
                    .clickable(enabled = !selected) { onTabSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) PrimaryBlue else TextMuted
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Combined Auth host (tabs swap the form in-place, no screen navigation)
// ---------------------------------------------------------------------------

/**
 * Single auth surface hosting both the Sign In and Sign Up forms. Switching the
 * top tab only swaps the inner form + header text (no activity transition).
 *
 * @param initialTab 0 = Sign Up, 1 = Sign In (default).
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    userRole: String,
    initialTab: Int = 1,
    onGoogleSignIn: () -> Unit,
    onGoogleSignUp: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialTab) }

    val authState by viewModel.authState.collectAsState()
    val authEvent by viewModel.authEvent.collectAsState()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                val msg = if (selectedTab == 1) "Login Successful!" else "Registration Successful!"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onAuthSuccess(state.role)
                viewModel.resetState()
            }

            is AuthState.Error -> Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            else -> {}
        }
    }

    LaunchedEffect(authEvent) {
        (authEvent as? AuthEvent.Message)?.let {
            Toast.makeText(context, it.text, Toast.LENGTH_LONG).show()
            viewModel.resetEvent()
        }
    }

    AuthBackground {
        val subtitle = if (selectedTab == 1)
            "Welcome back, sign in to continue"
        else
            "Create your account as $userRole"
        BrandHeader(subtitle = subtitle)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthTabs(selectedIndex = selectedTab) { index -> selectedTab = index }
                Spacer(modifier = Modifier.height(24.dp))

                // Only the inner form swaps — the shell, header and tabs persist.
                if (selectedTab == 1) {
                    SignInForm(
                        viewModel = viewModel,
                        loading = authState is AuthState.Loading,
                        onGoogleSignIn = onGoogleSignIn
                    )
                } else {
                    SignUpForm(
                        viewModel = viewModel,
                        userRole = userRole,
                        loading = authState is AuthState.Loading,
                        onGoogleSignUp = onGoogleSignUp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Sign In form
// ---------------------------------------------------------------------------

@Composable
private fun ColumnScope.SignInForm(
    viewModel: AuthViewModel,
    loading: Boolean,
    onGoogleSignIn: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val emailError = submitted && !isValidEmail(email)
    val passwordError = submitted && password.isBlank()

    AuthTextField(
        value = email,
        onValueChange = { email = it },
        label = "Email",
        leadingIcon = Icons.Filled.Email,
        keyboardType = KeyboardType.Email,
        isError = emailError,
        errorText = if (email.isBlank()) "Email is required" else "Enter a valid email"
    )
    Spacer(modifier = Modifier.height(4.dp))

    AuthTextField(
        value = password,
        onValueChange = { password = it },
        label = "Password",
        leadingIcon = Icons.Filled.Lock,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        isPassword = true,
        passwordVisible = passwordVisible,
        onTogglePassword = { passwordVisible = !passwordVisible },
        isError = passwordError,
        errorText = "Password is required"
    )

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Forgot Password?",
        color = PrimaryBlue,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .align(Alignment.End)
            .clickable { viewModel.sendPasswordReset(email.trim()) }
            .padding(vertical = 4.dp)
    )

    Spacer(modifier = Modifier.height(20.dp))
    PrimaryActionButton(text = "Sign In", loading = loading) {
        submitted = true
        if (isValidEmail(email) && password.isNotBlank()) {
            viewModel.signIn(email.trim(), password.trim())
        } else {
            Toast.makeText(context, "Please fix the highlighted fields", Toast.LENGTH_SHORT).show()
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    DividerWithText("Or continue with")
    Spacer(modifier = Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        SocialIcon(R.drawable.google_login_ic, onClick = onGoogleSignIn)
    }
}

// ---------------------------------------------------------------------------
// Sign Up form
// ---------------------------------------------------------------------------

@Composable
private fun SignUpForm(
    viewModel: AuthViewModel,
    userRole: String,
    loading: Boolean,
    onGoogleSignUp: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val nameError = submitted && name.isBlank()
    val emailError = submitted && !isValidEmail(email)
    val phoneError = submitted && phone.length < 10
    val passwordError = submitted && !isStrongEnough(password)
    val confirmError = submitted && confirmPassword != password

    AuthTextField(
        value = name,
        onValueChange = { name = it },
        label = "Full Name",
        leadingIcon = Icons.Filled.Person,
        isError = nameError,
        errorText = "Name is required"
    )
    Spacer(modifier = Modifier.height(4.dp))

    AuthTextField(
        value = email,
        onValueChange = { email = it },
        label = "Email",
        leadingIcon = Icons.Filled.Email,
        keyboardType = KeyboardType.Email,
        isError = emailError,
        errorText = if (email.isBlank()) "Email is required" else "Enter a valid email"
    )
    Spacer(modifier = Modifier.height(4.dp))

    AuthTextField(
        value = phone,
        onValueChange = {
            if (it.length <= 10 && it.all { c -> c.isDigit() }) phone = it
        },
        label = "Phone Number",
        leadingIcon = Icons.Filled.Phone,
        keyboardType = KeyboardType.Phone,
        isError = phoneError,
        errorText = if (phone.isBlank()) "Phone number is required" else "Enter a valid 10-digit number"
    )
    Spacer(modifier = Modifier.height(4.dp))

    AuthTextField(
        value = password,
        onValueChange = { password = it },
        label = "Password",
        leadingIcon = Icons.Filled.Lock,
        keyboardType = KeyboardType.Password,
        isPassword = true,
        passwordVisible = passwordVisible,
        onTogglePassword = { passwordVisible = !passwordVisible },
        isError = passwordError,
        errorText = "Use at least 6 characters"
    )
    Spacer(modifier = Modifier.height(4.dp))

    AuthTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = "Confirm Password",
        leadingIcon = Icons.Filled.CheckCircle,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        isPassword = true,
        passwordVisible = confirmVisible,
        onTogglePassword = { confirmVisible = !confirmVisible },
        isError = confirmError,
        errorText = "Passwords do not match"
    )

    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = acceptedTerms,
            onCheckedChange = { acceptedTerms = it },
            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
        )
        Text("I agree to the Terms & Privacy Policy", fontSize = 13.sp, color = TextMuted)
    }

    Spacer(modifier = Modifier.height(16.dp))
    PrimaryActionButton(text = "Sign Up", loading = loading) {
        submitted = true
        when {
            name.isBlank() || !isValidEmail(email) || phone.length < 10 ||
                    !isStrongEnough(password) || confirmPassword != password ->
                Toast.makeText(context, "Please fix the highlighted fields", Toast.LENGTH_SHORT)
                    .show()

            !acceptedTerms ->
                Toast.makeText(
                    context,
                    "Please accept the Terms & Privacy Policy",
                    Toast.LENGTH_SHORT
                ).show()

            else ->
                viewModel.signUp(
                    email.trim(),
                    password.trim(),
                    userRole,
                    name.trim(),
                    phone.trim()
                )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    DividerWithText("Or sign up with")
    Spacer(modifier = Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        SocialIcon(R.drawable.google_login_ic, onClick = onGoogleSignUp)
    }
}
