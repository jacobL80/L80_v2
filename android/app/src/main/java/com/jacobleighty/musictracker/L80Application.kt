package com.jacobleighty.musictracker

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.jacobleighty.musictracker.ui.DataChangeEvents
import com.jacobleighty.musictracker.ui.EditModeManager
import com.jacobleighty.musictracker.widget.enqueueWidgetRefresh
import kotlinx.coroutines.launch

/**
 * Widgets are cheap to get stale (backgrounded process, killed activity, another
 * device writing data), so refresh is driven centrally here rather than from each
 * screen: any time the app comes to the foreground, and any time any screen reports
 * a data change, regardless of which widget or screen caused it.
 */
class L80Application : Application() {
    override fun onCreate() {
        super.onCreate()

        EditModeManager.init(this)

        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        processLifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                enqueueWidgetRefresh(this@L80Application)
            }
        })

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            DataChangeEvents.flow.collect { enqueueWidgetRefresh(this@L80Application) }
        }
    }
}
