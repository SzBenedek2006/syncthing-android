package dev.benedek.syncthingandroid;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import dev.benedek.syncthingandroid.service.NotificationHandler;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class SyncthingModule {

    private final SyncthingApp mApp;

    public SyncthingModule(SyncthingApp app) {
        mApp = app;
    }

    @Provides
    @Singleton
    public SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mApp);
    }

    @Provides
    @Singleton
    public NotificationHandler getNotificationHandler() {
        return new NotificationHandler(mApp);
    }
}
