package dev.benedek.syncthingandroid.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import dev.benedek.syncthingandroid.R;
import dev.benedek.syncthingandroid.activities.FolderActivity;
import dev.benedek.syncthingandroid.activities.SyncthingActivity;
import dev.benedek.syncthingandroid.model.Folder;
import dev.benedek.syncthingandroid.service.Constants;
import dev.benedek.syncthingandroid.service.RestApi;
import dev.benedek.syncthingandroid.service.SyncthingService;
import dev.benedek.syncthingandroid.ui.FolderViewModel;
import dev.benedek.syncthingandroid.views.FoldersAdapter;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Displays a list of all existing folders.
 */
public class FolderListFragment extends ListFragment
        implements SyncthingService.OnServiceStateChangeListener, AdapterView.OnItemClickListener {

    private FoldersAdapter adapter;

    private Timer timer;

    @Override
    public void onPause() {
        super.onPause();
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void onServiceStateChange(SyncthingService.State currentState) {
        if (currentState != SyncthingService.State.ACTIVE)
            return;

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (getActivity() == null)
                    return;

                getActivity().runOnUiThread(FolderListFragment.this::updateList);
            }

        }, 0, Constants.GUI_UPDATE_INTERVAL);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true);
        setEmptyText(getString(R.string.folder_list_empty));
        getListView().setOnItemClickListener(this);
    }

    /**
     * Refreshes ListView by updating folders and info.
     * <p>
     * Also creates adapter if it doesn't exist yet.
     */
    private void updateList() {
        SyncthingActivity activity = (SyncthingActivity) getActivity();
        if (activity == null || getView() == null || activity.isFinishing()) {
            return;
        }
        RestApi restApi = activity.getApi();
        if (restApi == null || !restApi.isConfigLoaded()) {
            return;
        }
        List<Folder> folders = restApi.getFolders();
        if (folders == null) {
            return;
        }
        if (adapter == null) {
            adapter = new FoldersAdapter(activity);
            setListAdapter(adapter);
        }

        // Prevent scroll position reset due to list update from clear().
        adapter.setNotifyOnChange(false);
        adapter.clear();
        adapter.addAll(folders);
        adapter.updateFolderStatus(restApi);
        adapter.notifyDataSetChanged();
        setListShown(true);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(getActivity(), FolderActivity.class)
                .putExtra(FolderViewModel.EXTRA_IS_CREATE, false)
                .putExtra(FolderViewModel.EXTRA_FOLDER_ID, adapter.getItem(i).getId());
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.folder_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_folder) {
            Intent intent = new Intent(getActivity(), FolderActivity.class)
                    .putExtra(FolderViewModel.EXTRA_IS_CREATE, true);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}


