package eu.siacs.conversations.entities;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;

/**
 * Created by hlew on 7/23/15.
 */
public class AccountRemotium extends Account {
    private static final String TAG = "AccountRemotium";

    /* Keep in sync with EditAccountActivity.java and AccManager */
    private static final String EXTRAS_IP = "jabber_ip";

    /**
     * Standard {@code Account} constructor from parent class.
     *
     */
    public AccountRemotium() {
        super();
    }

    /**
     * Standard {@code Account} constructor from parent class.
     *
     * @param jid
     * @param password
     */
    public AccountRemotium(final Jid jid, final String password) {
        super(jid, password);
    }

    /**
     * Creates a new {@code Account} with name/value mappings from the JSON
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

    @Override
    /**
     * Returns a {@code Jid} of the {@code AccountRemotium} instance. Remotium accounts
     * will have either an IP address or DNS name as the domain part, so the
     * key/value store of the parent {@code Account} is checked for an IP address.
     * This will allow over-riding of standard DNS lookup behavior.
     * See XmppConnection.java for special handling of Remotium accounts.
     *
     * @return      the corresponding jid as an IP address if provided.
     *
     */
    public Jid getServer() {
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
     * @return      the corresponding jid for the AccountRemotium.
     *
     */
    public Jid getJid(String ipAddress) {
        Jid jid;
        try {
            jid = Jid.fromParts(super.getJid().getLocalpart(), ipAddress, super.getJid().getDomainpart());
        } catch (InvalidJidException e) {
            Log.e(TAG, "Invalid Jid conversion to IP: " + e);
            jid = null;
        }

        return (jid == null) ? super.getJid() : jid;

    }

}
