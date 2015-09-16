package eu.siacs.conversations.ui;

import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.R;
import eu.siacs.conversations.Config;
import eu.siacs.conversations.crypto.axolotl.AxolotlService;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.AccountRemotium;
import eu.siacs.conversations.services.XmppConnectionService.OnAccountUpdate;
import eu.siacs.conversations.ui.adapter.KnownHostsAdapter;
import eu.siacs.conversations.utils.CryptoHelper;
import eu.siacs.conversations.utils.UIHelper;
import eu.siacs.conversations.xmpp.OnKeyStatusUpdated;
import eu.siacs.conversations.xmpp.XmppConnection.Features;
import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;
import eu.siacs.conversations.xmpp.pep.Avatar;


public class EditAccountActivity extends XmppActivity implements OnAccountUpdate, OnKeyStatusUpdated {
	private static final String TAG = "EditAccount";

	private AutoCompleteTextView mAccountJid;
	private EditText mPassword;
	private EditText mPasswordConfirm;
	private EditText mAdvancedSettingServer;
	private EditText mAdvancedSettingPort;
	private CheckBox mRegisterNew;
	private CheckBox mAdvancedOptions;
	private CheckBox mUseTLS;
	private Button mCancelButton;
	private Button mSaveButton;
	private TableLayout mMoreTable;

	private LinearLayout mStats;
	private TextView mAdvancedServerInfo;
	private TextView mAdvancedPortInfo;
	private TextView mServerInfoSm;
	private TextView mServerInfoRosterVersion;
	private TextView mServerInfoCarbons;
	private TextView mServerInfoMam;
	private TextView mServerInfoCSI;
	private TextView mServerInfoBlocking;
	private TextView mServerInfoPep;
	private TextView mServerInfoHttpUpload;
	private TextView mSessionEst;
	private TextView mOtrFingerprint;
	private TextView mAxolotlFingerprint;
	private TextView mAccountJidLabel;
	private ImageView mAvatar;
	private RelativeLayout mOtrFingerprintBox;
	private RelativeLayout mAxolotlFingerprintBox;
	private ImageButton mOtrFingerprintToClipboardButton;
	private ImageButton mAxolotlFingerprintToClipboardButton;
	private ImageButton mRegenerateAxolotlKeyButton;
	private LinearLayout keys;
	private LinearLayout keysCard;

	private Jid jidToEdit;
	private Account mAccount;
	private String messageFingerprint;

	private boolean mFetchingAvatar = false;

	private final OnClickListener mSaveButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(final View v) {
			if (mAccount != null && mAccount.getStatus() == Account.State.DISABLED && !accountInfoEdited()) {
				mAccount.setOption(Account.OPTION_DISABLED, false);
				xmppConnectionService.updateAccount(mAccount);
				return;
			}
			final boolean registerNewAccount = mRegisterNew.isChecked() && !Config.DISALLOW_REGISTRATION_IN_UI;
			final boolean useAdvancedOptions = mAdvancedOptions.isChecked();
			final boolean useTLS = mUseTLS.isChecked();
			if (Config.DOMAIN_LOCK != null && mAccountJid.getText().toString().contains("@")) {
				mAccountJid.setError(getString(R.string.invalid_username));
				mAccountJid.requestFocus();
				return;
			}

			final Jid jid;
			try {
				if (Config.DOMAIN_LOCK != null) {
					jid = Jid.fromParts(mAccountJid.getText().toString(),Config.DOMAIN_LOCK,null);
				} else {
					jid = Jid.fromString(mAccountJid.getText().toString());
				}
			} catch (final InvalidJidException e) {
				if (Config.DOMAIN_LOCK != null) {
					mAccountJid.setError(getString(R.string.invalid_username));
				} else {
					mAccountJid.setError(getString(R.string.invalid_jid));
				}
				mAccountJid.requestFocus();
				return;
			}
			if (jid.isDomainJid()) {
				if (Config.DOMAIN_LOCK != null) {
					mAccountJid.setError(getString(R.string.invalid_username));
				} else {
					mAccountJid.setError(getString(R.string.invalid_jid));
				}
				mAccountJid.requestFocus();
				return;
			}
			final String password = mPassword.getText().toString();
			final String passwordConfirm = mPasswordConfirm.getText().toString();
			if (registerNewAccount) {
				if (!password.equals(passwordConfirm)) {
					mPasswordConfirm.setError(getString(R.string.passwords_do_not_match));
					mPasswordConfirm.requestFocus();
					return;
				}
			}
			final String server = mAdvancedSettingServer.getText().toString();
			final String port = mAdvancedSettingPort.getText().toString();

			if (mAccount != null) {
				if (useAdvancedOptions) {
						mAccount.setKey(Config.EXTRAS_IP, !server.isEmpty() ? server : null);
						mAccount.setKey(Config.EXTRAS_PORT, !port.isEmpty() ? port: null);
				}
				try {
					mAccount.setUsername(jid.hasLocalpart() ? jid.getLocalpart() : "");
					mAccount.setServer(jid.getDomainpart());
				} catch (final InvalidJidException ignored) {
					return;
				}
				mAccountJid.setError(null);
				mPasswordConfirm.setError(null);
				mAccount.setPassword(password);
				mAccount.setOption(Account.OPTION_REGISTER, registerNewAccount);
				mAccount.setOption(Account.OPTION_USETLS, useTLS);

				xmppConnectionService.updateAccount(mAccount);
			} else {
				if (xmppConnectionService.findAccountByJid(jid) != null) {
					mAccountJid.setError(getString(R.string.account_already_exists));
					mAccountJid.requestFocus();
					return;
				}

				/* Before adding Remotium accounts
				mAccount = new Account(jid.toBareJid(), password);
				*/
				mAccount = new AccountRemotium(jid.toBareJid(), password);
				if (useAdvancedOptions) {
					mAccount.setKey(Config.EXTRAS_IP, !server.isEmpty() ? server : null);
					mAccount.setKey(Config.EXTRAS_PORT, !port.isEmpty() ? port : null);
				}
				mAccount.setOption(Account.OPTION_USETLS, useTLS);
				mAccount.setOption(Account.OPTION_USECOMPRESSION, true);
				mAccount.setOption(Account.OPTION_REGISTER, registerNewAccount);
				xmppConnectionService.createAccount(mAccount);
			}
			if (jidToEdit != null && !mAccount.isOptionSet(Account.OPTION_DISABLED)) {
				finish();
			} else {
				updateSaveButton();
				updateAccountInformation(true);
			}

		}
	};
	private final OnClickListener mCancelButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(final View v) {
			finish();
		}
	};

	public void refreshUiReal() {
		invalidateOptionsMenu();
		if (mAccount != null
				&& mAccount.getStatus() != Account.State.ONLINE
				&& mFetchingAvatar) {
			startActivity(new Intent(getApplicationContext(),
						ManageAccountActivity.class));
			finish();
		} else if (jidToEdit == null && mAccount != null
				&& mAccount.getStatus() == Account.State.ONLINE) {
			if (!mFetchingAvatar) {
				mFetchingAvatar = true;
				xmppConnectionService.checkForAvatar(mAccount,
						mAvatarFetchCallback);
			}
		} else {
			updateSaveButton();
		}
		if (mAccount != null) {
			updateAccountInformation(false);
		}
	}

	@Override
	public void onAccountUpdate() {
		refreshUi();
	}
	private final UiCallback<Avatar> mAvatarFetchCallback = new UiCallback<Avatar>() {

		@Override
		public void userInputRequried(final PendingIntent pi, final Avatar avatar) {
			finishInitialSetup(avatar);
		}

		@Override
		public void success(final Avatar avatar) {
			finishInitialSetup(avatar);
		}

		@Override
		public void error(final int errorCode, final Avatar avatar) {
			finishInitialSetup(avatar);
		}
	};
	private final TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
			updateSaveButton();
		}

		@Override
		public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
		}

		@Override
		public void afterTextChanged(final Editable s) {

		}
	};

	private final OnClickListener mAvatarClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			if (mAccount != null) {
				final Intent intent = new Intent(getApplicationContext(),
						PublishProfilePictureActivity.class);
				intent.putExtra("account", mAccount.getJid().toBareJid().toString());
				startActivity(intent);
			}
		}
	};

	protected void finishInitialSetup(final Avatar avatar) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Intent intent;
				if (avatar != null) {
					intent = new Intent(getApplicationContext(),
							StartConversationActivity.class);
					if (xmppConnectionService != null && xmppConnectionService.getAccounts().size() == 1) {
						intent.putExtra("init", true);
					}
				} else {
					intent = new Intent(getApplicationContext(),
							PublishProfilePictureActivity.class);
					intent.putExtra("account", mAccount.getJid().toBareJid().toString());
					intent.putExtra("setup", true);
				}
				startActivity(intent);
				finish();
			}
		});
	}

	protected void updateSaveButton() {
		if (accountInfoEdited() && jidToEdit != null) {
			this.mSaveButton.setText(R.string.save);
			this.mSaveButton.setEnabled(true);
			this.mSaveButton.setTextColor(getPrimaryTextColor());
		} else if (mAccount != null && (mAccount.getStatus() == Account.State.CONNECTING || mFetchingAvatar)) {
			this.mSaveButton.setEnabled(false);
			this.mSaveButton.setTextColor(getSecondaryTextColor());
			this.mSaveButton.setText(R.string.account_status_connecting);
		} else if (mAccount != null && mAccount.getStatus() == Account.State.DISABLED) {
			this.mSaveButton.setEnabled(true);
			this.mSaveButton.setTextColor(getPrimaryTextColor());
			this.mSaveButton.setText(R.string.enable);
		} else {
			this.mSaveButton.setEnabled(true);
			this.mSaveButton.setTextColor(getPrimaryTextColor());
			if (jidToEdit != null) {
				if (mAccount != null && mAccount.isOnlineAndConnected()) {
					this.mSaveButton.setText(R.string.save);
					if (!accountInfoEdited()) {
						this.mSaveButton.setEnabled(false);
						this.mSaveButton.setTextColor(getSecondaryTextColor());
					}
				} else {
					this.mSaveButton.setText(R.string.connect);
				}
			} else {
				this.mSaveButton.setText(R.string.next);
			}
		}
	}

	protected boolean accountInfoEdited() {
		if (this.mAccount == null) {
			return false;
		}
		final String unmodified;
		if (Config.DOMAIN_LOCK != null) {
			unmodified = this.mAccount.getJid().getLocalpart();
		} else {
			unmodified = this.mAccount.getJid().toBareJid().toString();
		}
		return !unmodified.equals(this.mAccountJid.getText().toString()) ||
				!this.mAccount.getPassword().equals(this.mPassword.getText().toString())
				|| !getIpExtra().equals(
				this.mAdvancedSettingServer.getText().toString())
				|| !getPortExtra().equals(
				this.mAdvancedSettingPort.getText().toString())
				|| !(this.mAccount.isOptionSet(Account.OPTION_USETLS) ==
				this.mUseTLS.isChecked());
	}

	private String getExtra(String key) {
		String value;
		try {
			value = this.mAccount.getKeys().getString(key);
		} catch (JSONException e) {
			value = "";
		}
		return value;
	}

	@Override
	protected String getShareableUri() {
		if (mAccount!=null) {
			return mAccount.getShareableUri();
		} else {
			return "";
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_account);
		this.mAccountJid = (AutoCompleteTextView) findViewById(R.id.account_jid);
		this.mAccountJid.addTextChangedListener(this.mTextWatcher);
		this.mAccountJidLabel = (TextView) findViewById(R.id.account_jid_label);
		if (Config.DOMAIN_LOCK != null) {
			this.mAccountJidLabel.setText(R.string.username);
			this.mAccountJid.setHint(R.string.username_hint);
		}
		this.mPassword = (EditText) findViewById(R.id.account_password);
		this.mPassword.addTextChangedListener(this.mTextWatcher);
		this.mPasswordConfirm = (EditText) findViewById(R.id.account_password_confirm);
		this.mAdvancedSettingServer = (EditText) findViewById(R.id.account_server_setting);
		this.mAdvancedSettingServer.addTextChangedListener(this.mTextWatcher);
		this.mAdvancedSettingPort = (EditText) findViewById(R.id.account_port_setting);
		this.mAdvancedSettingPort.addTextChangedListener(this.mTextWatcher);
		this.mAdvancedServerInfo = (TextView) findViewById(R.id.account_server_desc);
		this.mAdvancedPortInfo = (TextView) findViewById(R.id.account_port_desc);
		this.mAvatar = (ImageView) findViewById(R.id.avater);
		this.mAvatar.setOnClickListener(this.mAvatarClickListener);
		this.mRegisterNew = (CheckBox) findViewById(R.id.account_register_new);
		this.mAdvancedOptions = (CheckBox) findViewById(R.id.account_settings_advanced_settings);
		this.mUseTLS = (CheckBox) findViewById(R.id.account_use_tls);
		this.mStats = (LinearLayout) findViewById(R.id.stats);
		this.mSessionEst = (TextView) findViewById(R.id.session_est);
		this.mServerInfoRosterVersion = (TextView) findViewById(R.id.server_info_roster_version);
		this.mServerInfoCarbons = (TextView) findViewById(R.id.server_info_carbons);
		this.mServerInfoMam = (TextView) findViewById(R.id.server_info_mam);
		this.mServerInfoCSI = (TextView) findViewById(R.id.server_info_csi);
		this.mServerInfoBlocking = (TextView) findViewById(R.id.server_info_blocking);
		this.mServerInfoSm = (TextView) findViewById(R.id.server_info_sm);
		this.mServerInfoPep = (TextView) findViewById(R.id.server_info_pep);
		this.mServerInfoHttpUpload = (TextView) findViewById(R.id.server_info_http_upload);
		this.mOtrFingerprint = (TextView) findViewById(R.id.otr_fingerprint);
		this.mOtrFingerprintBox = (RelativeLayout) findViewById(R.id.otr_fingerprint_box);
		this.mOtrFingerprintToClipboardButton = (ImageButton) findViewById(R.id.action_copy_to_clipboard);
		this.mAxolotlFingerprint = (TextView) findViewById(R.id.axolotl_fingerprint);
		this.mAxolotlFingerprintBox = (RelativeLayout) findViewById(R.id.axolotl_fingerprint_box);
		this.mAxolotlFingerprintToClipboardButton = (ImageButton) findViewById(R.id.action_copy_axolotl_to_clipboard);
		this.mRegenerateAxolotlKeyButton = (ImageButton) findViewById(R.id.action_regenerate_axolotl_key);
		this.keysCard = (LinearLayout) findViewById(R.id.other_device_keys_card);
		this.keys = (LinearLayout) findViewById(R.id.other_device_keys);
		this.mSaveButton = (Button) findViewById(R.id.save_button);
		this.mCancelButton = (Button) findViewById(R.id.cancel_button);
		this.mSaveButton.setOnClickListener(this.mSaveButtonClickListener);
		this.mCancelButton.setOnClickListener(this.mCancelButtonClickListener);
		this.mMoreTable = (TableLayout) findViewById(R.id.server_info_more);

		final OnCheckedChangeListener OnCheckedShowConfirmPassword = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView,
					final boolean isChecked) {
				if (isChecked) {
					mPasswordConfirm.setVisibility(View.VISIBLE);
				} else {
					mPasswordConfirm.setVisibility(View.GONE);
				}
				updateSaveButton();
			}
		};
		final OnCheckedChangeListener OnCheckedShowAdvancedSettings = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView,
										 final boolean isChecked) {
				if (isChecked) {
					mAdvancedServerInfo.setVisibility(View.VISIBLE);
					mAdvancedSettingServer.setVisibility(View.VISIBLE);
					mAdvancedPortInfo.setVisibility(View.VISIBLE);
					mAdvancedSettingPort.setVisibility(View.VISIBLE);
					mUseTLS.setVisibility(View.VISIBLE);
				} else {
					mAdvancedServerInfo.setVisibility(View.GONE);
					mAdvancedSettingServer.setVisibility(View.GONE);
					mAdvancedPortInfo.setVisibility(View.GONE);
					mAdvancedSettingPort.setVisibility(View.GONE);
					mUseTLS.setVisibility(View.GONE);
				}
				updateSaveButton();
			}

		};
		final OnCheckedChangeListener OnCheckedUseTls = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView,
										 final boolean isChecked) {
				updateSaveButton();
			}

		};
		this.mRegisterNew.setOnCheckedChangeListener(OnCheckedShowConfirmPassword);
		if (Config.DISALLOW_REGISTRATION_IN_UI) {
			this.mRegisterNew.setVisibility(View.GONE);
		}
		this.mAdvancedOptions.setOnCheckedChangeListener(OnCheckedShowAdvancedSettings);
		this.mUseTLS.setOnCheckedChangeListener(OnCheckedUseTls);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.editaccount, menu);
		final MenuItem showQrCode = menu.findItem(R.id.action_show_qr_code);
		final MenuItem showBlocklist = menu.findItem(R.id.action_show_block_list);
		final MenuItem showMoreInfo = menu.findItem(R.id.action_server_info_show_more);
		final MenuItem changePassword = menu.findItem(R.id.action_change_password_on_server);
		final MenuItem clearDevices = menu.findItem(R.id.action_clear_devices);
		if (mAccount != null && mAccount.isOnlineAndConnected()) {
			if (!mAccount.getXmppConnection().getFeatures().blocking()) {
				showBlocklist.setVisible(false);
			}
			if (!mAccount.getXmppConnection().getFeatures().register()) {
				changePassword.setVisible(false);
			}
			Set<Integer> otherDevices = mAccount.getAxolotlService().getOwnDeviceIds();
			if (otherDevices == null || otherDevices.isEmpty()) {
				clearDevices.setVisible(false);
			}
		} else {
			showQrCode.setVisible(false);
			showBlocklist.setVisible(false);
			showMoreInfo.setVisible(false);
			changePassword.setVisible(false);
			clearDevices.setVisible(false);
		}
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (getIntent() != null) {
			try {
				this.jidToEdit = Jid.fromString(getIntent().getStringExtra("jid"));
			} catch (final InvalidJidException | NullPointerException ignored) {
				this.jidToEdit = null;
			}
			this.messageFingerprint = getIntent().getStringExtra("fingerprint");
			if (this.jidToEdit != null) {
				this.mRegisterNew.setVisibility(View.GONE);
				if (getActionBar() != null) {
					getActionBar().setTitle(getString(R.string.account_details));
				}
			} else {
				this.mAvatar.setVisibility(View.GONE);
				if (getActionBar() != null) {
					getActionBar().setTitle(R.string.action_add_account);
				}
			}
		}
	}

	@Override
	protected void onBackendConnected() {
		if (this.jidToEdit != null) {
			this.mAccount = xmppConnectionService.findAccountByJid(jidToEdit);
			updateAccountInformation(true);
		} else if (this.xmppConnectionService.getAccounts().size() == 0) {
			if (getActionBar() != null) {
				getActionBar().setDisplayHomeAsUpEnabled(false);
				getActionBar().setDisplayShowHomeEnabled(false);
				getActionBar().setHomeButtonEnabled(false);
			}
			this.mCancelButton.setEnabled(false);
			this.mCancelButton.setTextColor(getSecondaryTextColor());
		}
		if (Config.DOMAIN_LOCK == null) {
			final KnownHostsAdapter mKnownHostsAdapter = new KnownHostsAdapter(this,
					android.R.layout.simple_list_item_1,
					xmppConnectionService.getKnownHosts());
			this.mAccountJid.setAdapter(mKnownHostsAdapter);
		}

		/* Account configuration from intent. */
		Intent intent = getIntent();
		String action = intent.getAction();
		if (action != null && action.equals(Config.ACTION_JABBER_ADD_ACCOUNT)) {
			new AutoConfigureAccountTask().execute(intent);
			finish();
		}

		updateSaveButton();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_show_block_list:
				final Intent showBlocklistIntent = new Intent(this, BlocklistActivity.class);
				showBlocklistIntent.putExtra("account", mAccount.getJid().toString());
				startActivity(showBlocklistIntent);
				break;
			case R.id.action_server_info_show_more:
				mMoreTable.setVisibility(item.isChecked() ? View.GONE : View.VISIBLE);
				item.setChecked(!item.isChecked());
				break;
			case R.id.action_change_password_on_server:
				final Intent changePasswordIntent = new Intent(this, ChangePasswordActivity.class);
				changePasswordIntent.putExtra("account", mAccount.getJid().toString());
				startActivity(changePasswordIntent);
				break;
			case R.id.action_clear_devices:
				showWipePepDialog();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * Configure a jabber account from Intent. If the account already exists,
	 * reconfigure the account with new information.
	 * TODO: Need to add a mechanism for deleting an account.
	 */
	private class AutoConfigureAccountTask extends AsyncTask<Intent, Void, Void> {
		@Override
		protected Void doInBackground(Intent... intents) {
			/* TODO: Need to check to make sure this doesn't get stuck in a loop */
			if (xmppConnectionService == null) {
				// This should never happen
				Log.e(TAG, "Need to wait for XMPPConnectionService reconnect.");
				connectToBackend();
			}

			Intent intent = intents[0];
			populateAccountFromIntent(intent);

			/* TODO: You're leaking the password here. */
			Log.v(TAG, "Contents of new account: " + mAccount.getContentValues().toString());

			if (xmppConnectionService.findAccountByJid(mAccount.getJid()) != null) {
				/* Update account */
				Log.v(TAG, "Tried to configure an account which already exists: " + mAccount.getJid());
				Log.v(TAG, "Updating account with jid: " + mAccount.getJid());
				xmppConnectionService.updateAccount(mAccount);
			} else {
				/* Add new account */
				Log.v(TAG, "Adding account with jid: " + mAccount.getJid());
				xmppConnectionService.createAccount(mAccount);
			}

			final Avatar avatar = null;
			finishInitialSetup(avatar);

			return null;
		}

		private void populateAccountFromIntent(Intent intent) {

			String jabberId = intent.getStringExtra(Config.EXTRAS_JID);
			String jabberIp = intent.getStringExtra(Config.EXTRAS_IP);
			String jabberPassword = intent.getStringExtra(Config.EXTRAS_PASSWORD);
			boolean jabberUseTls = intent.getBooleanExtra(Config.EXTRAS_USE_TLS, true);


			/* For debugging only using adb */
			String jabberPortString = intent.getStringExtra(Config.EXTRAS_PORT);
			Log.v(TAG, "Jabber port string: " + jabberPortString);
			//int jabberPort = Integer.parseInt(jabberPortString);
			/* ***************************** */

			Log.v(TAG, "Configuring jabber account for "
					+ jabberId + " on ip: " + jabberIp + ":" + jabberPort);
			final Jid jid;
			try {
				jid = Jid.fromString(jabberId);
			} catch (final InvalidJidException e) {
				Log.e(TAG, "Bad jid provided:" + jabberId);
				return;
			}

			/* IP address and Port are NOT required by the XMPP protocol, but in our case will
			 * be used to override the standard domain lookup function used by the protocol.
			 * See XMPPConnection.java and AccountRemotium.java for special handling when using
			 * RVMP auto-configuration.
			 */
			JSONObject extras = null;
			try {
				extras = new JSONObject();
				extras.put(Config.EXTRAS_IP, jabberIp);
				extras.put(Config.EXTRAS_PORT, "" + jabberPort);
			} catch (JSONException e) {
				// Just keeping with the documentation.
				throw new RuntimeException(e);
			} finally {
				if (extras == null) {
					mAccount = new AccountRemotium(jid.toBareJid(), jabberPassword);
				} else {
					mAccount = new AccountRemotium(jid.toBareJid(), jabberPassword, extras.toString());
				}
			}
			/* TODO: Note that compression is disabled for now */
			mAccount.setOption(Account.OPTION_USETLS, jabberUseTls);
			mAccount.setOption(Account.OPTION_USECOMPRESSION, false);
		}

	}

	private void updateAccountInformation(boolean init) {
		if (init) {
			if (Config.DOMAIN_LOCK != null) {
				this.mAccountJid.setText(this.mAccount.getJid().getLocalpart());
			} else {
				this.mAccountJid.setText(this.mAccount.getJid().toBareJid().toString());
			}
			this.mPassword.setText(this.mAccount.getPassword());
		}
		AccountRemotium rAccount = (AccountRemotium) mAccount;
		this.mAccountJid.setTextKeepState(this.mAccount.getJid().toBareJid().toString());
		this.mPassword.setTextKeepState(this.mAccount.getPassword());
		if (getExtra(Config.EXTRAS_IP).isEmpty() && getExtra(Config.EXTRAS_PORT).isEmpty()) {
			mAdvancedOptions.setChecked(false);
		}
		else {
			mAdvancedOptions.setChecked(true);
		}
		this.mAdvancedSettingServer.setTextKeepState(getExtra(Config.EXTRAS_IP));
		this.mAdvancedSettingPort.setTextKeepState(getExtra(Config.EXTRAS_PORT));
		this.mUseTLS.setChecked(this.mAccount.isOptionSet(Account.OPTION_USETLS));

		if (this.jidToEdit != null) {
			this.mAvatar.setVisibility(View.VISIBLE);
			this.mAvatar.setImageBitmap(avatarService().get(this.mAccount, getPixel(72)));
		}
		if (this.mAccount.isOptionSet(Account.OPTION_REGISTER)) {
			this.mRegisterNew.setVisibility(View.VISIBLE);
			this.mRegisterNew.setChecked(true);
			this.mPasswordConfirm.setText(this.mAccount.getPassword());
		} else {
			this.mRegisterNew.setVisibility(View.GONE);
			this.mRegisterNew.setChecked(false);
		}
		if (this.mAccount.isOnlineAndConnected() && !this.mFetchingAvatar) {
			this.mStats.setVisibility(View.VISIBLE);
			this.mSessionEst.setText(UIHelper.readableTimeDifferenceFull(this, this.mAccount.getXmppConnection()
						.getLastSessionEstablished()));
			Features features = this.mAccount.getXmppConnection().getFeatures();
			if (features.rosterVersioning()) {
				this.mServerInfoRosterVersion.setText(R.string.server_info_available);
			} else {
				this.mServerInfoRosterVersion.setText(R.string.server_info_unavailable);
			}
			if (features.carbons()) {
				this.mServerInfoCarbons.setText(R.string.server_info_available);
			} else {
				this.mServerInfoCarbons
					.setText(R.string.server_info_unavailable);
			}
			if (features.mam()) {
				this.mServerInfoMam.setText(R.string.server_info_available);
			} else {
				this.mServerInfoMam.setText(R.string.server_info_unavailable);
			}
			if (features.csi()) {
				this.mServerInfoCSI.setText(R.string.server_info_available);
			} else {
				this.mServerInfoCSI.setText(R.string.server_info_unavailable);
			}
			if (features.blocking()) {
				this.mServerInfoBlocking.setText(R.string.server_info_available);
			} else {
				this.mServerInfoBlocking.setText(R.string.server_info_unavailable);
			}
			if (features.sm()) {
				this.mServerInfoSm.setText(R.string.server_info_available);
			} else {
				this.mServerInfoSm.setText(R.string.server_info_unavailable);
			}
			if (features.pep()) {
				AxolotlService axolotlService = this.mAccount.getAxolotlService();
				if (axolotlService != null && axolotlService.isPepBroken()) {
					this.mServerInfoPep.setText(R.string.server_info_broken);
				} else {
					this.mServerInfoPep.setText(R.string.server_info_available);
				}
			} else {
				this.mServerInfoPep.setText(R.string.server_info_unavailable);
			}
			if (features.httpUpload()) {
				this.mServerInfoHttpUpload.setText(R.string.server_info_available);
			} else {
				this.mServerInfoHttpUpload.setText(R.string.server_info_unavailable);
			}
			final String otrFingerprint = this.mAccount.getOtrFingerprint();
			if (otrFingerprint != null) {
				this.mOtrFingerprintBox.setVisibility(View.VISIBLE);
				this.mOtrFingerprint.setText(CryptoHelper.prettifyFingerprint(otrFingerprint));
				this.mOtrFingerprintToClipboardButton
					.setVisibility(View.VISIBLE);
				this.mOtrFingerprintToClipboardButton
					.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(final View v) {

							if (copyTextToClipboard(otrFingerprint, R.string.otr_fingerprint)) {
								Toast.makeText(
										EditAccountActivity.this,
										R.string.toast_message_otr_fingerprint,
										Toast.LENGTH_SHORT).show();
							}
						}
					});
			} else {
				this.mOtrFingerprintBox.setVisibility(View.GONE);
			}
			final String axolotlFingerprint = this.mAccount.getAxolotlService().getOwnFingerprint();
			if (axolotlFingerprint != null) {
				this.mAxolotlFingerprintBox.setVisibility(View.VISIBLE);
				this.mAxolotlFingerprint.setText(CryptoHelper.prettifyFingerprint(axolotlFingerprint));
				this.mAxolotlFingerprintToClipboardButton
						.setVisibility(View.VISIBLE);
				this.mAxolotlFingerprintToClipboardButton
						.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(final View v) {

								if (copyTextToClipboard(axolotlFingerprint, R.string.omemo_fingerprint)) {
									Toast.makeText(
											EditAccountActivity.this,
											R.string.toast_message_omemo_fingerprint,
											Toast.LENGTH_SHORT).show();
								}
							}
						});
				if (Config.SHOW_REGENERATE_AXOLOTL_KEYS_BUTTON) {
					this.mRegenerateAxolotlKeyButton
							.setVisibility(View.VISIBLE);
					this.mRegenerateAxolotlKeyButton
							.setOnClickListener(new View.OnClickListener() {

								@Override
								public void onClick(final View v) {
									showRegenerateAxolotlKeyDialog();
								}
							});
				}
			} else {
				this.mAxolotlFingerprintBox.setVisibility(View.GONE);
			}
			final String ownFingerprint = mAccount.getAxolotlService().getOwnFingerprint();
			boolean hasKeys = false;
			keys.removeAllViews();
			for (final String fingerprint : mAccount.getAxolotlService().getFingerprintsForOwnSessions()) {
				if(ownFingerprint.equals(fingerprint)) {
					continue;
				}
				boolean highlight = fingerprint.equals(messageFingerprint);
				hasKeys |= addFingerprintRow(keys, mAccount, fingerprint, highlight);
			}
			if (hasKeys) {
				keysCard.setVisibility(View.VISIBLE);
			} else {
				keysCard.setVisibility(View.GONE);
			}
		} else {
			if (this.mAccount.errorStatus()) {
				this.mAccountJid.setError(getString(this.mAccount.getStatus().getReadableId()));
				if (init || !accountInfoEdited()) {
					this.mAccountJid.requestFocus();
				}
			} else {
				this.mAccountJid.setError(null);
			}
			this.mStats.setVisibility(View.GONE);
		}
	}

	public void showRegenerateAxolotlKeyDialog() {
		Builder builder = new Builder(this);
		builder.setTitle("Regenerate Key");
		builder.setIconAttribute(android.R.attr.alertDialogIcon);
		builder.setMessage("Are you sure you want to regenerate your Identity Key? (This will also wipe all established sessions and contact Identity Keys)");
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mAccount.getAxolotlService().regenerateKeys();
					}
				});
		builder.create().show();
	}

	public void showWipePepDialog() {
		Builder builder = new Builder(this);
		builder.setTitle(getString(R.string.clear_other_devices));
		builder.setIconAttribute(android.R.attr.alertDialogIcon);
		builder.setMessage(getString(R.string.clear_other_devices_desc));
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.accept),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mAccount.getAxolotlService().wipeOtherPepDevices();
					}
				});
		builder.create().show();
	}

	@Override
	public void onKeyStatusUpdated() {
		refreshUi();
	}
}
