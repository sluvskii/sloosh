package com.slooshfilm.app.ui.widget

import android.content.Intent
import android.widget.RemoteViewsService

class NewReleasesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NewReleasesRemoteViewsFactory(this.applicationContext, intent)
    }
}
