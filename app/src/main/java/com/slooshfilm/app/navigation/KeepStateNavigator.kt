package com.slooshfilm.app.navigation

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDestination
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator

@Navigator.Name("keep_state_fragment")
class KeepStateNavigator(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val containerId: Int
) : Navigator<FragmentNavigator.Destination>() {

    override fun createDestination(): FragmentNavigator.Destination {
        return FragmentNavigator.Destination(this)
    }

    override fun navigate(
        destination: FragmentNavigator.Destination,
        args: Bundle?,
        navOptions: androidx.navigation.NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): NavDestination? {
        val tag = destination.id.toString()
        val transaction = fragmentManager.beginTransaction()

        val current = fragmentManager.primaryNavigationFragment
        if (current != null) {
            transaction.setMaxLifecycle(current, Lifecycle.State.STARTED)
            transaction.hide(current)
        }

        var fragment = fragmentManager.findFragmentByTag(tag)
        if (fragment == null) {
            val className = destination.className
            fragment = Fragment.instantiate(context, className, args)
            transaction.add(containerId, fragment, tag)
        } else {
            transaction.show(fragment)
        }

        transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
        transaction.setPrimaryNavigationFragment(fragment)
        transaction.setReorderingAllowed(true)
        transaction.commitAllowingStateLoss()
        return destination
    }

    override fun popBackStack(): Boolean {
        return false
    }
}
