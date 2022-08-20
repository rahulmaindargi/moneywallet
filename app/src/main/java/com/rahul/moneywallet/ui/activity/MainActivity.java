/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rahul.moneywallet.ui.activity;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.rahul.moneywallet.service.syncworkers.devicesync.NSDClientHelper.CLIENT_WORKER_NAME;
import static com.rahul.moneywallet.service.syncworkers.devicesync.NSDServiceRegistrationHelper.SERVER_WORKER_NAME;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.rahul.moneywallet.R;
import com.rahul.moneywallet.broadcast.LocalAction;
import com.rahul.moneywallet.model.ColorIcon;
import com.rahul.moneywallet.model.Money;
import com.rahul.moneywallet.model.WalletAccount;
import com.rahul.moneywallet.service.BackupHandlerIntentService;
import com.rahul.moneywallet.service.syncworkers.RefreshAllSMSListWorker;
import com.rahul.moneywallet.service.syncworkers.RefreshSMSFormatsWorker;
import com.rahul.moneywallet.service.syncworkers.devicesync.NSDClientHelper;
import com.rahul.moneywallet.service.syncworkers.devicesync.NSDServiceRegistrationHelper;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.DataContentProvider;
import com.rahul.moneywallet.storage.preference.PreferenceManager;
import com.rahul.moneywallet.ui.activity.base.BaseActivity;
import com.rahul.moneywallet.ui.fragment.base.NavigableFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.BudgetMultiPanelViewPagerFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.CategoryMultiPanelViewPagerFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.DebtMultiPanelViewPagerFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.EventMultiPanelViewPagerFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.ModelMultiPanelViewPagerFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.PersonMultiPanelFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.PlaceMultiPanelFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.RecurrenceMultiPanelViewPagerFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.SavingMultiPanelViewPagerFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.SettingMultiPanelFragment;
import com.rahul.moneywallet.ui.fragment.multipanel.TransactionMultiPanelViewPagerFragment;
import com.rahul.moneywallet.ui.fragment.singlepanel.OverviewSinglePanelFragment;
import com.rahul.moneywallet.ui.view.theme.ITheme;
import com.rahul.moneywallet.ui.view.theme.ThemeEngine;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;
import com.rahul.moneywallet.ui.view.theme.ThemedRecyclerView;
import com.rahul.moneywallet.utils.IconLoader;

import java.util.Locale;

public class MainActivity extends BaseActivity implements DrawerController, AccountHeader.OnAccountHeaderListener, Drawer.OnDrawerItemClickListener
        , LoaderManager.LoaderCallbacks<Cursor> {

    private static final String SAVED_SELECTION = "MainActivity::current_selection";

    private static final int LOADER_WALLETS = 1;

    private static final int ID_SECTION_TRANSACTIONS = 0;
    private static final int ID_SECTION_CATEGORIES = 1;
    private static final int ID_SECTION_OVERVIEW = 2;
    private static final int ID_SECTION_DEBTS = 3;
    private static final int ID_SECTION_BUDGETS = 4;
    private static final int ID_SECTION_SAVINGS = 5;
    private static final int ID_SECTION_EVENTS = 6;
    private static final int ID_SECTION_RECURRENCES = 7;
    private static final int ID_SECTION_MODELS = 8;
    private static final int ID_SECTION_PLACES = 9;
    private static final int ID_SECTION_PEOPLE = 10;
    private static final int ID_SECTION_CALCULATOR = 11;
    private static final int ID_SECTION_CONVERTER = 12;
    private static final int ID_SECTION_ATM = 13;
    private static final int ID_SECTION_BANK = 14;
    private static final int ID_SECTION_SETTING = 15;
    private static final int ID_SECTION_SUPPORT_DEVELOPER = 16;
    private static final int ID_SECTION_ABOUT = 17;
    private static final int ID_SECTION_REFRESH_SMS_FORMATS = 18;
    private static final int ID_SECTION_REFRESH_ALL_SMS = 19;
    private static final int ID_SECTION_SYNC_NEARBY_DEVICE_CLIENT = 20;
    private static final int ID_SECTION_SYNC_NEARBY_DEVICE_SERVER = 21;

    private final static int ID_ACTION_NEW_WALLET = 1;
    private final static int ID_ACTION_MANAGE_WALLET = 2;
    private static final String WORK_COMPLETED = "WORK_COMPLETED";
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                int action = intent.getIntExtra(BackupHandlerIntentService.ACTION, 0);
                if (action == BackupHandlerIntentService.ACTION_RESTORE) {
                    LoaderManager.getInstance(MainActivity.this).restartLoader(LOADER_WALLETS, null, MainActivity.this);
                }
            }
        }

    };
    private AccountHeader mAccountHeader;
    private Drawer mDrawer;
    private long mCurrentSelection;
    private Fragment mCurrentFragment;
    private Cursor mCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();

        initializeUi();
        loadUi(savedInstanceState);
        registerReceiver();
    }

    /**
     * Initialize all the ui elements of the activity.
     */
    private void initializeUi() {
        setContentView(R.layout.activity_root_container);
        initializeNavigationDrawer();
    }

    /**
     * This method must be called during the initialization of the activity in order to
     * setup the account header and the navigation drawer.
     */
    private void initializeNavigationDrawer() {
        mAccountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.colorPrimary)
                .withOnAccountHeaderListener(this)
                .build();
        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(mAccountHeader)
                .addDrawerItems(
                        createDrawerItem(ID_SECTION_TRANSACTIONS, R.drawable.ic_shopping_cart_24dp, R.string.menu_transaction),
                        createDrawerItem(ID_SECTION_CATEGORIES, R.drawable.ic_table_large_24dp, R.string.menu_category),
                        createDrawerItem(ID_SECTION_OVERVIEW, R.drawable.ic_equalizer_24dp, R.string.menu_overview),
                        //createDrawerItem(ID_SECTION_DEBTS, R.drawable.ic_debt_24dp, R.string.menu_debt),
                        createDrawerItem(ID_SECTION_BUDGETS, R.drawable.ic_budget_24dp, R.string.menu_budget),
                        createDrawerItem(ID_SECTION_SAVINGS, R.drawable.ic_saving_24dp, R.string.menu_saving),
                        createDrawerItem(ID_SECTION_EVENTS, R.drawable.ic_assistant_photo_24dp, R.string.menu_event),
                        createDrawerItem(ID_SECTION_RECURRENCES, R.drawable.ic_restore_24dp, R.string.menu_recurrences),
                        //createDrawerItem(ID_SECTION_MODELS, R.drawable.ic_bookmark_black_24dp, R.string.menu_models),
                        createDrawerItem(ID_SECTION_PLACES, R.drawable.ic_place_24dp, R.string.menu_place),
                        createDrawerItem(ID_SECTION_PEOPLE, R.drawable.ic_people_black_24dp, R.string.menu_people),
                        new DividerDrawerItem(),
                        createDrawerItem(ID_SECTION_CALCULATOR, R.drawable.ic_calculator_24dp, R.string.menu_calculator),
                        //createDrawerItem(ID_SECTION_CONVERTER, R.drawable.ic_converter_24dp, R.string.menu_converter),
                        //createDrawerItem(ID_SECTION_ATM, R.drawable.ic_credit_card_24dp, R.string.menu_search_atm),
                        //createDrawerItem(ID_SECTION_BANK, R.drawable.ic_account_balance_24dp, R.string.menu_search_bank),
                        new DividerDrawerItem(),
                        createDrawerItem(ID_SECTION_SETTING, R.drawable.ic_settings_24dp, R.string.menu_setting),
                        createDrawerItem(ID_SECTION_REFRESH_SMS_FORMATS, R.drawable.ic_refresh_black_24dp, R.string.menu_refresh_sms_formats),
                        createDrawerItem(ID_SECTION_REFRESH_ALL_SMS, R.drawable.ic_refresh_black_24dp, R.string.menu_refresh_all_sms),
                        createDrawerItem(ID_SECTION_SYNC_NEARBY_DEVICE_CLIENT, R.drawable.ic_restore_24dp, R.string.menu_sync_near_by_device_client),
                        createDrawerItem(ID_SECTION_SYNC_NEARBY_DEVICE_SERVER, R.drawable.ic_restore_24dp, R.string.menu_sync_near_by_device_server),
                        //createDrawerItem(ID_SECTION_SUPPORT_DEVELOPER, R.drawable.ic_favorite_border_black_24dp, R.string.menu_support_developer),
                        createDrawerItem(ID_SECTION_ABOUT, R.drawable.ic_info_outline_24dp, R.string.menu_about)
                )
                .withOnDrawerItemClickListener(this)
                .build();
    }

    /**
     * This method is responsible to create a new PrimaryDrawerItem entry for the navigation drawer.
     *
     * @param identifier integer id of the item.
     * @param icon       drawable resource of the icon of the item.
     * @param name       string resource of the name of the item.
     * @return the created drawer item.
     */
    private IDrawerItem createDrawerItem(int identifier, @DrawableRes int icon, @StringRes int name) {
        return new PrimaryDrawerItem()
                .withIdentifier(identifier)
                .withIconTintingEnabled(true)
                .withIcon(icon)
                .withName(name);
    }

    private void loadUi(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCurrentSelection = savedInstanceState.getLong(SAVED_SELECTION);
        } else {
            // TODO maybe we can let the user to specify a preference for the first section to load
            mCurrentSelection = ID_SECTION_TRANSACTIONS;
        }
        mDrawer.setSelection(mCurrentSelection, true);
        LoaderManager.getInstance(this).restartLoader(LOADER_WALLETS, null, this);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(LocalAction.ACTION_BACKUP_SERVICE_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);
    }

    /**
     * Store all the instance information in order to restore them if the activity is recreated.
     * The only information to store here is the current section loaded. The fragment will manage
     * the lifecycle internally, no need to save his state here.
     *
     * @param savedState of the current instance of the activity.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putLong(SAVED_SELECTION, mCurrentSelection);
    }

    /**
     * Override this method to check when the back button is pressed if the drawer is open.
     * If open it will be closed.
     */
    @Override
    public void onBackPressed() {
        if (mDrawer != null && mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
        } else if (mCurrentFragment instanceof NavigableFragment) {
            if (!((NavigableFragment) mCurrentFragment).navigateBack()) {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    /**
     * Set toolbar for this activity.
     *
     * @param toolbar to set as main toolbar.
     */
    @Override
    public void setToolbar(Toolbar toolbar) {
        mDrawer.setToolbar(this, toolbar, true);
    }

    /**
     * Set the lock mode for the activity drawer.
     *
     * @param lockMode to set to the navigation drawer.
     */
    @Override
    public void setDrawerLockMode(int lockMode) {
        mDrawer.getDrawerLayout().setDrawerLockMode(lockMode);
    }

    /**
     * This method is called by the navigation drawer whenever a profile is clicked.
     *
     * @param view    of the clicked profile.
     * @param profile clicked.
     * @param current true if is the current profile.
     * @return always false.
     */
    @Override
    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
        long id = profile.getIdentifier();
        if (profile instanceof ProfileDrawerItem) {
            PreferenceManager.setCurrentWallet(this, id);
        } else if (profile instanceof ProfileSettingDrawerItem) {
            if (id == ID_ACTION_NEW_WALLET) {
                Intent intent = new Intent(MainActivity.this, NewEditWalletActivity.class);
                startActivity(intent);
            } else if (id == ID_ACTION_MANAGE_WALLET) {
                Intent intent = new Intent(MainActivity.this, WalletListActivity.class);
                startActivity(intent);
            }
        }
        return false;
    }

    /**
     * Callback when a drawer item is clicked.
     *
     * @param view       of the drawer item.
     * @param position   of the drawer item.
     * @param drawerItem clicked.
     * @return true if the event was consumed.
     */
    @Override
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        if (drawerItem instanceof PrimaryDrawerItem) {
            int identifier = (int) drawerItem.getIdentifier();
            switch (identifier) {
                case ID_SECTION_CALCULATOR:
                    startActivity(new Intent(this, CalculatorActivity.class));
                    break;
                case ID_SECTION_CONVERTER:
                    startActivity(new Intent(this, CurrencyConverterActivity.class));
                    break;
                case ID_SECTION_ATM:
                    showAtmSearchDialog();
                    break;
                case ID_SECTION_BANK:
                    showBankSearchDialog();
                    break;
                case ID_SECTION_SUPPORT_DEVELOPER:
                    startActivity(new Intent(this, DonationActivity.class));
                    break;
                case ID_SECTION_ABOUT:
                    startActivity(new Intent(this, AboutActivity.class));
                    break;
                case ID_SECTION_REFRESH_SMS_FORMATS:
                    checkPermission();

                    Operation operationFormat = WorkManager.getInstance(this).enqueueUniqueWork("RefreshSMSFormatsWorker", ExistingWorkPolicy.KEEP, OneTimeWorkRequest.from(RefreshSMSFormatsWorker.class));

                    operationFormat.getState().observe(this, state -> handleRefreshState(state, "SMS Formats Refreshed successfully", "SMS Formats Refresh Failed Try again.", "SMSFormatRefresh", "RefreshSMSFormatsWorker"));

                    break;
                case ID_SECTION_REFRESH_ALL_SMS:
                    checkPermission();

                    Operation operationSms = WorkManager.getInstance(this).enqueueUniqueWork("RefreshAllSMSListWorker", ExistingWorkPolicy.KEEP, OneTimeWorkRequest.from(RefreshAllSMSListWorker.class));
                    operationSms.getState().observe(this, state -> handleRefreshState(state, "SMS Refreshed successfully", "SMS Refresh Failed Try again.", "SMSRefresh", "RefreshAllSMSListWorker"));
                    break;
                case ID_SECTION_SYNC_NEARBY_DEVICE_CLIENT:
                    checkPermission();
                    NSDClientHelper clientHelper = new NSDClientHelper(this);
                    clientHelper.startDiscovery();
                    WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(CLIENT_WORKER_NAME)
                            .observe(this, infos -> infos.stream()
                                    .filter(info -> !info.getState().isFinished())
                                    .findAny().ifPresent(info ->
                                            workerCompletionToast(CLIENT_WORKER_NAME, "Data Sent", "Data Send failed", clientHelper::stopDiscovery)
                                    )
                            );

                    break;
                case ID_SECTION_SYNC_NEARBY_DEVICE_SERVER:
                    checkPermission();
                    NSDServiceRegistrationHelper helper = new NSDServiceRegistrationHelper(this);
                    helper.registerService();
                    WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(SERVER_WORKER_NAME)
                            .observe(this, infos -> infos.stream()
                                    .filter(info -> !info.getState().isFinished())
                                    .findAny().ifPresent(info ->
                                            workerCompletionToast(SERVER_WORKER_NAME, "Data Received", "Data Receive failed", helper::deRegisterService)
                                    )
                            );
                    break;
                default:
                    mCurrentSelection = identifier;
                    loadSection(identifier);
                    return false;
            }
        }
        mDrawer.closeDrawer();
        mDrawer.setSelection(mCurrentSelection, false);
        return true;
    }

    private void workerCompletionToast(String serverWorkerName, String successMessage, String failMessage, Runnable extraHandler) {
        createNotificationChannel();
        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(serverWorkerName)
                .observe(this, infos -> infos.stream()
                        .filter(info -> info.getState().isFinished())
                        .forEach(info -> {
                            if (extraHandler != null) {
                                extraHandler.run();
                            }
                            if (info.getState() == WorkInfo.State.SUCCEEDED) {
                                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                                showNotification(serverWorkerName, successMessage);
                            } else {
                                Toast.makeText(this, failMessage, Toast.LENGTH_SHORT).show();
                                showNotification(serverWorkerName, failMessage);
                            }
                        }));
    }

    private void showNotification(String serverWorkerName, String message) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), WORK_COMPLETED)
                .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                .setContentTitle("Action Completed!") // title for notification
                .setContentText(message) // message for notification
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);// clear notification after click
        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
        manager.cancel(100);
        manager.notify(100, mBuilder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = "WorkManager Notification Channel";
        String description = "Notification about Background work";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(WORK_COMPLETED, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void handleRefreshState(Operation.State state, String successMessage, String failureMessage, String operation, String workName) {
        if (state instanceof Operation.State.SUCCESS) {
            Toast.makeText(this, "Operation started", Toast.LENGTH_SHORT).show();
            workerCompletionToast(workName, successMessage, failureMessage, null);
        } else if (state instanceof Operation.State.FAILURE) {
            Throwable throwable = ((Operation.State.FAILURE) state).getThrowable();
            Toast.makeText(this, "Operation not started. Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(operation, failureMessage, throwable);

        }
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_SMS) != PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.GET_ACCOUNTS
                    },
                    2);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECEIVE_SMS) != PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.GET_ACCOUNTS},
                    1);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.GET_ACCOUNTS},
                    3);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.GET_ACCOUNTS) != PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.GET_ACCOUNTS},
                    3);
        }
    }

    private void showAtmSearchDialog() {
        ThemedDialog.buildMaterialDialog(this)
                .title(R.string.title_atm_search)
                .input(R.string.hint_atm_name, 0, false, (dialog, input) -> {
                    Uri mapUri = Uri.parse("geo:0,0?q=atm " + input);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                    try {
                        startActivity(mapIntent);
                    } catch (ActivityNotFoundException ignore) {
                        showActivityNotFoundDialog();
                    }
                }).show();
    }

    private void showBankSearchDialog() {
        ThemedDialog.buildMaterialDialog(this)
                .title(R.string.title_bank_search)
                .input(R.string.hint_bank_name, 0, false, (dialog, input) -> {
                    Uri mapUri = Uri.parse("geo:0,0?q=bank " + input);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                    try {
                        startActivity(mapIntent);
                    } catch (ActivityNotFoundException ignore) {
                        showActivityNotFoundDialog();
                    }
                }).show();
    }

    private void showActivityNotFoundDialog() {
        ThemedDialog.buildMaterialDialog(this)
                .title(R.string.title_error)
                .content(R.string.message_error_activity_not_found)
                .positiveText(android.R.string.ok)
                .show();
    }

    /**
     * Load the fragment of the specified section inside the frame of the activity.
     * If the fragment is already in the stack of the fragment manager this method will
     * reuse it without spending time in recreating a new one.
     *
     * @param identifier of the section.
     */
    private void loadSection(int identifier) {
        FragmentManager manager = getSupportFragmentManager();
        String tag = getTagById(identifier);
        mCurrentFragment = manager.findFragmentByTag(tag);
        if (mCurrentFragment == null) {
            mCurrentFragment = buildFragmentById(identifier);
        }
        manager.beginTransaction().replace(R.id.fragment_container, mCurrentFragment, tag).commit();
    }

    /**
     * Generate a unique string as tag to identify every fragment into the fragment manager.
     *
     * @param identifier of the drawer item.
     * @return a unique tag.
     */
    private String getTagById(int identifier) {
        return String.format(Locale.ENGLISH, "MainActivity::drawer::%d", identifier);
    }

    /**
     * This method creates a new fragment of the specified section.
     *
     * @param identifier of the section.
     * @return the new created fragment.
     * @throws IllegalArgumentException if the provided id is not a
     *                                  valid section identifier.
     */
    private Fragment buildFragmentById(int identifier) {
        switch (identifier) {
            case ID_SECTION_TRANSACTIONS:
                return new TransactionMultiPanelViewPagerFragment();
            case ID_SECTION_CATEGORIES:
                return new CategoryMultiPanelViewPagerFragment();
            case ID_SECTION_OVERVIEW:
                return new OverviewSinglePanelFragment();
            case ID_SECTION_DEBTS:
                return new DebtMultiPanelViewPagerFragment();
            case ID_SECTION_BUDGETS:
                return new BudgetMultiPanelViewPagerFragment();
            case ID_SECTION_SAVINGS:
                return new SavingMultiPanelViewPagerFragment();
            case ID_SECTION_EVENTS:
                return new EventMultiPanelViewPagerFragment();
            case ID_SECTION_RECURRENCES:
                return new RecurrenceMultiPanelViewPagerFragment();
            case ID_SECTION_MODELS:
                return new ModelMultiPanelViewPagerFragment();
            case ID_SECTION_PLACES:
                return new PlaceMultiPanelFragment();
            case ID_SECTION_PEOPLE:
                return new PersonMultiPanelFragment();
            case ID_SECTION_SETTING:
                return new SettingMultiPanelFragment();
            default:
                throw new IllegalArgumentException("Invalid section id: " + identifier);
        }
    }

    /**
     * Query content resolver to retrieve all wallets from the database.
     *
     * @param id   of the loader.
     * @param args bundle of arguments.
     * @return the cursor loader that will retrieve the content from the database.
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = new String[]{
                Contract.Wallet.ID,
                Contract.Wallet.NAME,
                Contract.Wallet.ICON,
                Contract.Wallet.COUNT_IN_TOTAL,
                Contract.Wallet.CURRENCY,
                Contract.Wallet.START_MONEY,
                Contract.Wallet.TOTAL_MONEY,
                Contract.Wallet.ARCHIVED
        };
        Uri uri = DataContentProvider.CONTENT_WALLETS;
        String sortOrder = Contract.Wallet.INDEX + " ASC, " + Contract.Wallet.NAME + " ASC";
        return new CursorLoader(this, uri, projection, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        mCursor = cursor;
        mAccountHeader.clear();
        ITheme theme = ThemeEngine.getTheme();
        int iconColor = theme.isDark() ? Color.parseColor("#8AFFFFFF") : Color.parseColor("#8A000000");
        int selectedIconColor = theme.getColorPrimary();
        int textColor = theme.isDark() ? Color.parseColor("#DEFFFFFF") : Color.parseColor("#DE000000");
        int selectedTextColor = theme.getColorPrimary();
        int selectedColor = theme.isDark() ? Color.parseColor("#202020") : Color.parseColor("#E8E8E8");
        if (mCursor != null) {
            int indexWalletId = mCursor.getColumnIndexOrThrow(Contract.Wallet.ID);
            int indexWalletName = mCursor.getColumnIndexOrThrow(Contract.Wallet.NAME);
            int indexWalletIcon = mCursor.getColumnIndexOrThrow(Contract.Wallet.ICON);
            int indexCurrency = mCursor.getColumnIndexOrThrow(Contract.Wallet.CURRENCY);
            int indexWalletInitial = mCursor.getColumnIndexOrThrow(Contract.Wallet.START_MONEY);
            int indexWalletTotal = mCursor.getColumnIndexOrThrow(Contract.Wallet.TOTAL_MONEY);
            int indexWalletArchived = mCursor.getColumnIndexOrThrow(Contract.Wallet.ARCHIVED);
            for (int i = 0, c = 0; i < mCursor.getCount(); i++) {
                mCursor.moveToPosition(i);
                if (cursor.getInt(indexWalletArchived) == 0) {
                    String currency = cursor.getString(indexCurrency);
                    long money = cursor.getLong(indexWalletInitial) + cursor.getLong(indexWalletTotal);
                    mAccountHeader.addProfile(new WalletAccount()
                                    .withIdentifier(cursor.getLong(indexWalletId))
                                    .withName(cursor.getString(indexWalletName))
                                    .withIcon(this, IconLoader.parse(cursor.getString(indexWalletIcon)))
                                    .withMoney(currency, money)
                                    .withNameShown(true)
                                    .withTextColor(textColor)
                                    .withSelectedTextColor(selectedTextColor)
                                    .withSelectedColor(selectedColor)
                            , c);
                    c += 1;
                }
            }
        }
        if (mAccountHeader.getProfiles().size() > 0) {
            mAccountHeader.setSelectionFirstLine(null);
            mAccountHeader.setSelectionSecondLine(null);
            ProfileDrawerItem totalAccount = createTotalWalletAccount(mCursor);
            if (totalAccount != null) {
                totalAccount.withTextColor(textColor);
                totalAccount.withSelectedTextColor(selectedTextColor);
                totalAccount.withSelectedColor(selectedColor);
                mAccountHeader.addProfiles(totalAccount);
            }
            long currentWalletId = PreferenceManager.getCurrentWallet();
            if (currentWalletId == PreferenceManager.NO_CURRENT_WALLET) {
                // if no wallet is set as current wallet, let the drawer to select the  Last (i.e. Total Wallet)
                // wallet found and then fire the onProfileChanged callback that will automatically
                // register the id inside the PreferenceManager and notify the changed to all the
                // observer registered at that uri.
                IProfile profile = mAccountHeader.getProfiles().get(mAccountHeader.getProfiles().size() - 1);
                mAccountHeader.setActiveProfile(profile, true);
            } else {
                mAccountHeader.setActiveProfile(currentWalletId);
            }
        } else {
            mAccountHeader.setSelectionFirstLine(getString(R.string.msg_no_wallet_found));
            mAccountHeader.setSelectionSecondLine(getString(R.string.msg_add_one_wallet));
        }
        mAccountHeader.addProfiles(
                new ProfileSettingDrawerItem()
                        .withIdentifier(ID_ACTION_NEW_WALLET)
                        .withIcon(R.drawable.ic_add_24dp)
                        .withName(getString(R.string.action_new_wallet))
                        .withIconTinted(true)
                        .withIconColor(iconColor)
                        .withTextColor(textColor)
                        .withSelectedColor(selectedColor),
                new ProfileSettingDrawerItem()
                        .withIdentifier(ID_ACTION_MANAGE_WALLET)
                        .withIcon(R.drawable.ic_settings_24dp)
                        .withName(getString(R.string.action_manage_wallets))
                        .withIconTinted(true)
                        .withIconColor(iconColor)
                        .withTextColor(textColor)
                        .withSelectedColor(selectedColor)
        );
    }

    /**
     * Create a total wallet profile from returned cursor.
     *
     * @param cursor not null that contains all available wallets.
     * @return the total wallet profile if it can be created, null otherwise.
     */
    private ProfileDrawerItem createTotalWalletAccount(@NonNull Cursor cursor) {
        Money money = new Money();
        int indexCurrency = mCursor.getColumnIndexOrThrow(Contract.Wallet.CURRENCY);
        int indexInTotal = mCursor.getColumnIndexOrThrow(Contract.Wallet.COUNT_IN_TOTAL);
        int indexWalletInitial = mCursor.getColumnIndexOrThrow(Contract.Wallet.START_MONEY);
        int indexWalletTotal = mCursor.getColumnIndexOrThrow(Contract.Wallet.TOTAL_MONEY);
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            if (cursor.getInt(indexInTotal) == 1) {
                String currency = cursor.getString(indexCurrency);
                long amount = cursor.getLong(indexWalletInitial) + cursor.getLong(indexWalletTotal);
                money.addMoney(currency, amount);
            }
        }
        if (money.getNumberOfCurrencies() > 0) {
            String name = getString(R.string.total_wallet_name);
            return new WalletAccount()
                    .withIdentifier(PreferenceManager.TOTAL_WALLET_ID)
                    .withName(name)
                    .withIcon(this, new ColorIcon("#000000", name.substring(0, 1)))
                    .withMoney(money)
                    .withNameShown(true);
        }
        return null;
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mAccountHeader.clear();
        if (mCursor != null) {
            if (!mCursor.isClosed()) {
                mCursor.close();
            }
            mCursor = null;
        }
    }

    @Override
    protected void onThemeSetup(ITheme theme) {
        super.onThemeSetup(theme);
        applyNavigationDrawerTheme(theme);
    }

    private void applyNavigationDrawerTheme(ITheme theme) {
        applyNavigationDrawerHeaderTheme(theme);
        applyNavigationDrawerBodyTheme(theme);
    }

    private void applyNavigationDrawerHeaderTheme(ITheme theme) {
        int backgroundColor = theme.getColorPrimary();
        int textColor = theme.getBestTextColor(backgroundColor);
        mAccountHeader.setBackground(new ColorDrawable(theme.getColorPrimary()));
        View headerView = mAccountHeader.getView();
        TextView nameTextView = headerView.findViewById(com.mikepenz.materialdrawer.R.id.material_drawer_account_header_name);
        TextView emailTextView = headerView.findViewById(com.mikepenz.materialdrawer.R.id.material_drawer_account_header_email);
        ImageView switcherImageView = headerView.findViewById(com.mikepenz.materialdrawer.R.id.material_drawer_account_header_text_switcher);
        if (nameTextView != null) {
            nameTextView.setTextColor(textColor);
        }
        if (emailTextView != null) {
            emailTextView.setTextColor(textColor);
        }
        if (switcherImageView != null) {
            switcherImageView.setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void applyNavigationDrawerBodyTheme(ITheme theme) {
        RecyclerView recyclerView = mDrawer.getRecyclerView();
        ThemedRecyclerView.applyTheme(recyclerView, theme);
        recyclerView.setBackgroundColor(theme.getDrawerBackgroundColor());
        int iconColor = theme.getDrawerIconColor();
        int selectedIconColor = theme.getDrawerSelectedIconColor();
        int textColor = theme.getDrawerTextColor();
        int selectedTextColor = theme.getDrawerSelectedTextColor();
        int selectedColor = theme.getDrawerSelectedItemColor();
        for (IDrawerItem item : mDrawer.getDrawerItems()) {
            if (item instanceof PrimaryDrawerItem) {
                ((PrimaryDrawerItem) item).withIconColor(iconColor);
                ((PrimaryDrawerItem) item).withTextColor(textColor);
                ((PrimaryDrawerItem) item).withSelectedIconColor(selectedIconColor);
                ((PrimaryDrawerItem) item).withSelectedTextColor(selectedTextColor);
                ((PrimaryDrawerItem) item).withSelectedColor(selectedColor);
            }
        }
        for (IProfile profile : mAccountHeader.getProfiles()) {
            if (profile instanceof ProfileDrawerItem) {
                ((ProfileDrawerItem) profile).withTextColor(textColor);
                ((ProfileDrawerItem) profile).withSelectedTextColor(selectedTextColor);
                ((ProfileDrawerItem) profile).withSelectedColor(selectedColor);
            } else if (profile instanceof ProfileSettingDrawerItem) {
                ((ProfileSettingDrawerItem) profile).withIconColor(iconColor);
                ((ProfileSettingDrawerItem) profile).withTextColor(textColor);
                ((ProfileSettingDrawerItem) profile).withSelectedColor(selectedColor);
            }
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    //  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onThemeStatusBar(ITheme theme) {
        mDrawer.getDrawerLayout().setStatusBarBackgroundColor(theme.getColorPrimaryDark());
    }
}