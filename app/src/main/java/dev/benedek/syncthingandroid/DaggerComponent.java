package dev.benedek.syncthingandroid;

import dev.benedek.syncthingandroid.activities.FirstStartActivity;
import dev.benedek.syncthingandroid.activities.FolderPickerActivity;
import dev.benedek.syncthingandroid.activities.MainActivity;
import dev.benedek.syncthingandroid.activities.ShareActivity;
import dev.benedek.syncthingandroid.activities.ThemedAppCompatActivity;
import dev.benedek.syncthingandroid.receiver.AppConfigReceiver;
import dev.benedek.syncthingandroid.service.RunConditionMonitor;
import dev.benedek.syncthingandroid.service.EventProcessor;
import dev.benedek.syncthingandroid.service.NotificationHandler;
import dev.benedek.syncthingandroid.service.RestApi;
import dev.benedek.syncthingandroid.service.SyncthingRunnable;
import dev.benedek.syncthingandroid.service.SyncthingService;
import dev.benedek.syncthingandroid.util.Languages;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {SyncthingModule.class})
public interface DaggerComponent {

    void inject(SyncthingApp app);
    void inject(MainActivity activity);
    void inject(FirstStartActivity activity);
    void inject(FolderPickerActivity activity);
    void inject(Languages languages);
    void inject(SyncthingService service);
    void inject(RunConditionMonitor runConditionMonitor);
    void inject(EventProcessor eventProcessor);
    void inject(SyncthingRunnable syncthingRunnable);
    void inject(NotificationHandler notificationHandler);
    void inject(AppConfigReceiver appConfigReceiver);
    void inject(RestApi restApi);
    void inject(ShareActivity activity);
    void inject(ThemedAppCompatActivity activity);
}
