package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.UnavailableQuickConnectState
import org.jellyfin.androidtv.databinding.FragmentUserLoginBinding
import org.jellyfin.androidtv.ui.startup.UserLoginViewModel
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class UserLoginFragment : Fragment() {
	companion object {
		const val ARG_USERNAME = "user_name"
		const val ARG_SERVER_ID = "server_id"
		const val ARG_SKIP_QUICKCONNECT = "skip_quickconnect"
		const val TAG_LOGIN_METHOD = "login_method"
	}

	private val userLoginViewModel: UserLoginViewModel by sharedViewModel()
	private lateinit var binding: FragmentUserLoginBinding

	private val usernameArgument get() = arguments?.getString(ARG_USERNAME)?.ifBlank { null }
	private val serverIdArgument get() = arguments?.getString(ARG_SERVER_ID)?.ifBlank { null }
	private val skipQuickConnect get() = arguments?.getBoolean(ARG_SKIP_QUICKCONNECT) ?: false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		userLoginViewModel.forcedUsername = usernameArgument
		userLoginViewModel.setServer(serverIdArgument?.toUUIDOrNull())
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = FragmentUserLoginBinding.inflate(inflater, container, false)

		binding.cancel.setOnClickListener { parentFragmentManager.popBackStack() }
		binding.useCredentials.setOnClickListener { setLoginMethod<UserLoginCredentialsFragment>() }
		binding.useQuickconnect.setOnClickListener { setLoginMethod<UserLoginQuickConnectFragment>() }

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		userLoginViewModel.clearLoginState()

		// Open initial fragment
		if (skipQuickConnect) setLoginMethod<UserLoginCredentialsFragment>()
		else setLoginMethod<UserLoginQuickConnectFragment>()

		// Update "connecting to ..." text
		lifecycleScope.launch {
			userLoginViewModel.server.collect { server ->
				val name = server?.name ?: "Jellyfin"
				binding.subtitle.text = getString(R.string.login_connect_to, name)
			}
		}

		// Disable QuickConnect when unavailable
		lifecycleScope.launch {
			userLoginViewModel.quickConnectState.collect { state ->
				binding.useQuickconnect.isEnabled = state != UnavailableQuickConnectState
				if (state == UnavailableQuickConnectState) setLoginMethod<UserLoginCredentialsFragment>()
			}
		}
	}

	private inline fun <reified T : Fragment> setLoginMethod() {
		val currentFragment = childFragmentManager.findFragmentByTag(TAG_LOGIN_METHOD)
		if (currentFragment is T) return

		childFragmentManager.commit {
			setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
			replace<T>(binding.loginMethod.id, TAG_LOGIN_METHOD)
		}

		// Hide button for active fragment
		binding.useCredentials.isVisible = T::class != UserLoginCredentialsFragment::class
		binding.useQuickconnect.isVisible = T::class != UserLoginQuickConnectFragment::class
	}
}
