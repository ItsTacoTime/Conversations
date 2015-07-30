package eu.siacs.conversations.entities;

import android.database.Cursor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;
/**
 * Created by hlew on 7/23/15.
 */
public class AccountRemotium extends Account {
    private static final String LOG_TAG = "AccountRemotium";

    /* Keep in sync with EditAccountActivity.java and AccManager.java */
    private static final String EXTRAS_IP = "jabber_ip";

    /**
     * Constructs {@code Account} from parent constructor.
     *
     */
    public AccountRemotium() {
        super();
    }

    /**
     * Constructs {@code Account} from parent constructor.
     *
     * @param jid a Jid object describing a jabber account.
     * @param password a string containing a password
     */
    public AccountRemotium(final Jid jid, final String password) {
        super(jid, password);
    }

    /**
     * Constructor creates an {@code Account} with key/value mappings from the JSON
     * string.
     *
     * @param jid a Jid object describing a jabber account.
     * @param password a string containing a password
     * @param keys a JSON-encoded string containing an object.
     *
     */
    public AccountRemotium(final Jid jid, final String password, final String keys) {
        super(java.util.UUID.randomUUID().toString(), jid,
                password, 0, null, keys, null);
    }

    /**
     * Constructor maps to {@code Account} from parent constructor.
     *
     * See parent.
     *
     */
    public AccountRemotium(final String uuid, final Jid jid,
                   final String password, final int options, final String rosterVersion, final String keys,
                   final String avatar) {
        super(uuid, jid, password, options, rosterVersion, keys, avatar);
    }

    /**
     * Returns a {@code Jid} of the {@code AccountRemotium} instance. Remotium accounts
     * will have either an IP address or DNS name as the domain part, so the
     * key/value store of the parent {@code Account} is checked for an IP address.
     * This will allow avoiding the standard DNS lookup behavior.
     * See XmppConnection.java for special handling of Remotium accounts.
     *
     * @return Jid  The corresponding Jid as an IP address if provided.
     *
     */
    public Jid getServerOrIp() {
        final JSONObject keys = super.getKeys();
        if (keys.has("jabber_ip")) {
            String ip_address = null;
            try {
                ip_address = keys.getString("jabber_ip");
            } catch (JSONException ignored) {
                // this should never happen
            }
            if (ip_address != null) {
                return getJid(ip_address).toDomainJid();
            }
        }

        return super.getJid().toDomainJid();
    }

    /**
     * Returns a {@code Jid} with IP address replacing the domain part.
     *
     * @return Jid  The corresponding Domain Jid for the AccountRemotium.
     *              If an IP address is available, this replaces the domain part.
     *              example: test@example.com with an ip address 1.1.1.1
     *                       will be converted to test@1.1.1.1.com
     */
    private Jid getJid(String ipAddress) {
        Jid jid;
        try {
            jid = Jid.fromParts(super.getJid().getLocalpart(), ipAddress, super.getJid().getDomainpart());
        } catch (InvalidJidException e) {
            Log.e(LOG_TAG, "Invalid Jid conversion to IP: " + e);
            jid = null;
        }

        return (jid == null) ? super.getJid() : jid;

    }

    @Override
    /**
     * Extracts the port number from {@AccountRemotium} in the key/value
     * store. The value only exists if it was sent through an intent.
     *
     * @return int The port configured to the account.
     */
    public int getPort() {
        final JSONObject keys = super.getKeys();
        if (keys.has("jabber_port")) {
            int port;
            try {
                port = Integer.valueOf(keys.getString("jabber_port"));

                /* Validate the port. */
                if (port <= 0 && port > 65535) {
                    port = super.getPort();
                }
            } catch (JSONException ignored) {
                Log.e(LOG_TAG, "JSONException", ignored);
                port= super.getPort();
            }
            Log.v(LOG_TAG, "Using custom port: " + port);
            return port;
        }

        return super.getPort();
    }

    /* Returns an {@code Account} for compatibility with the original
     * Conversations application. To preserve overloading, we need to create
     * AccountRemotium instead of Account during backend data saving and restoration.
     * See DatabaseBackend.java for edits.
     *
     *  @param  Cursor  Sqlite database cursor for Accounts.
     *  @return Account Instantiates a new {@code AccountRemotium}
     */
    public static Account fromCursor(final Cursor cursor) {
        Jid jid = null;
        try {
            jid = Jid.fromParts(cursor.getString(cursor.getColumnIndex(USERNAME)),
                    cursor.getString(cursor.getColumnIndex(SERVER)), "mobile");
        } catch (final InvalidJidException ignored) {
        }
        return new AccountRemotium(cursor.getString(cursor.getColumnIndex(UUID)),
                jid,
                cursor.getString(cursor.getColumnIndex(PASSWORD)),
                cursor.getInt(cursor.getColumnIndex(OPTIONS)),
                cursor.getString(cursor.getColumnIndex(ROSTERVERSION)),
                cursor.getString(cursor.getColumnIndex(KEYS)),
                cursor.getString(cursor.getColumnIndex(AVATAR)));
    }

}
