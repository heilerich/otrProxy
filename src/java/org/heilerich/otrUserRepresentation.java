package org.heilerich;

import java.io.File;
import java.io.IOException;
import java.util.regex.PatternSyntaxException;
import java.util.HashMap;

import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.security.KeyPair;
import java.security.PublicKey;
import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.OtrKeyManagerStore;
import net.java.otr4j.OtrKeyManagerImpl;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;

import org.bouncycastle.util.encoders.Base64;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;

/**
 * otrProxy plugin.
 * 
 * @author Heilerich
 */
public class otrUserRepresentation {

	private JID owner;
	private OtrEngineImpl engine;
	private MyOtrEngineHost host;
	private MyOtrEngineListener listener;
	private HashMap<String,SessionID> sessions;
	private OtrKeyManagerImpl keyManager;
	private DefaultPropertiesStore store;
	private static final Logger Log = LoggerFactory.getLogger(MyOtrEngineHost.class);
	
	public otrUserRepresentation(JID user, String keyfile) {
		owner = user;
		try{
			store = new DefaultPropertiesStore(keyfile);
		}catch(IOException ioe){
			Log.error("Otr-Proxy: Failed to open key-store.dat", ioe);
		}
		keyManager = new OtrKeyManagerImpl(store);
		host = new MyOtrEngineHost(new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ERROR_START_AKE), keyManager);
		engine = new OtrEngineImpl(host);
		sessions = new HashMap<String,SessionID>();
		listener = new MyOtrEngineListener(engine, keyManager);
		engine.addOtrEngineListener(listener);
	}
	
	public KeyPair getKeyPair(SessionID paramSessionID) {
		KeyPair pair = keyManager.loadLocalKeyPair(paramSessionID);
		if(pair == null) {
			keyManager.generateLocalKeyPair(paramSessionID);
			return keyManager.loadLocalKeyPair(paramSessionID);
		} else {
			return pair;
		}
	}
	
	private SessionID getSession(JID buddy) {
		SessionID session = sessions.get(buddy.toBareJID());
		if(session!=null) {
			return session;
		} else {
			sessions.put(buddy.toBareJID(),new SessionID(owner.toBareJID(),buddy.toBareJID(),"XMPP"));
			session = sessions.get(buddy.toBareJID());
			return session;
		}
	}
	
	private void destroySession(JID buddy) {
		sessions.remove(buddy.toBareJID());
	}
	
	public void startSession(JID buddy) {
		engine.startSession(getSession(buddy));
	}
	
	public void endSession(JID buddy) {
		engine.endSession(getSession(buddy));
		destroySession(buddy);
	}
	
	public void refreshSession(JID buddy) {
		engine.refreshSession(getSession(buddy));
	}
	
	public String getSessionStatus(JID buddy) {
		SessionStatus status = engine.getSessionStatus(getSession(buddy));
		if(status == SessionStatus.ENCRYPTED) {
			return "encrypted";
		}
		if(status == SessionStatus.PLAINTEXT) {
			return "unencrypted";
		}
		if(status == SessionStatus.FINISHED) {
			return "finished";
		}
		return "undefined";
	}
	
	public String transformRecv(JID buddy, String msg) {
		return engine.transformReceiving(getSession(buddy),msg);
	}
	
	public String transformSend(JID buddy, String msg) {
		return engine.transformSending(getSession(buddy),msg);
	}

	public void verify(JID buddy) {
		keyManager.verify(getSession(buddy));
	}
	
	public void unverify(JID buddy) {
		keyManager.unverify(getSession(buddy));
	}
	
	public boolean isVerified(JID buddy) {
		getKeyPair(getSession(buddy));
		return keyManager.isVerified(getSession(buddy));
	}
	
	public String getRemoteFingerprint(JID buddy) {
		return keyManager.getRemoteFingerprint(getSession(buddy));
	}
	
	public String getLocalFingerprint(JID buddy) {
		getKeyPair(getSession(buddy));
		return keyManager.getLocalFingerprint(getSession(buddy));
	}

}

class MyOtrEngineListener implements OtrEngineListener {
	private MessageRouter messageRouter;

	public void sessionStatusChanged(SessionID sessionID) {
		String status = getSessionStatus(sessionID);
		if(status=="encrypted") {
			PublicKey loadkey = keyManager.loadRemotePublicKey(sessionID);
			PublicKey newkey = engine.getRemotePublicKey(sessionID);
			String loadfp = fingerprint(loadkey);
			String newfp = fingerprint(newkey);
			String veri = "";
			if(loadfp!=null) {
				if(loadfp.equals(newfp))
				{
					boolean isVerified = keyManager.isVerified(sessionID);
					if(isVerified)
						veri = "verified.";
					else
						veri = "known but unverified.";
				} else {
					veri = "unknown. The old key was overwritten.";
					keyManager.savePublicKey(sessionID, newkey);
				}
			} else {
				veri = "unknown. No old key is known.";
				keyManager.savePublicKey(sessionID, newkey);
			}
			sendMessage(new JID(sessionID.getUserID()),new JID(sessionID.getAccountID()),"[otr-server] status changed to " + status + ". The Fingerprint is " + veri);
		} else {
			sendMessage(new JID(sessionID.getUserID()),new JID(sessionID.getAccountID()),"[otr-server] status changed to " + status);
		}
	}
	
	private String getSessionStatus(SessionID sessid) {
		SessionStatus status = engine.getSessionStatus(sessid);
		if(status == SessionStatus.ENCRYPTED) {
			return "encrypted";
		}
		if(status == SessionStatus.PLAINTEXT) {
			return "unencrypted";
		}
		if(status == SessionStatus.FINISHED) {
			return "finished";
		}
		return "undefined";
	}
	
	private String fingerprint(PublicKey pk) {
		if(pk != null) {
			try {
					return new OtrCryptoEngineImpl().getFingerprint(pk);
			} catch (OtrCryptoException e) {
					e.printStackTrace();
					return null;
			}
		} else {
			return null;
		}
	}
	
	private OtrEngineImpl engine;
	private OtrKeyManagerImpl keyManager;
	
	public MyOtrEngineListener(OtrEngineImpl hostengine, OtrKeyManagerImpl keyMgr) {
		engine = hostengine;
		keyManager = keyMgr;
		messageRouter = XMPPServer.getInstance().getMessageRouter();
	}
	
	private void sendMessage(JID fromuser, JID touser, String body) {
		Message message = createServerMessage(fromuser, touser, body);
		messageRouter.route(message);
	}

	private Message createServerMessage(JID fromuser, JID touser, String body) {
		Message message = new Message();
		message.setTo(touser);
		message.setFrom(fromuser);
		message.setBody(body);
		return message;
	}
}

class MyOtrEngineHost implements OtrEngineHost {
	private OtrKeyManagerImpl keyManager;
	
	public MyOtrEngineHost(OtrPolicy policy, OtrKeyManagerImpl keyMgr) {
		this.policy = policy;
		messageRouter = XMPPServer.getInstance().getMessageRouter();
		keyManager = keyMgr;
	}

	private OtrPolicy policy;
	private MessageRouter messageRouter;

	public OtrPolicy getSessionPolicy(SessionID ctx) {
		return this.policy;
	}

	public void injectMessage(SessionID sessionID, String msg) {
		sendMessage(new JID(sessionID.getAccountID()),new JID(sessionID.getUserID()),msg);
	}

	public void showError(SessionID sessionID, String error) {
		sendMessage(new JID(sessionID.getUserID()),new JID(sessionID.getAccountID()),"[otr-server] Error: " + error);
	}

	public void showWarning(SessionID sessionID, String warning) {
		sendMessage(new JID(sessionID.getUserID()),new JID(sessionID.getAccountID()),"[otr-server] Warning: " + warning);
	}

	public KeyPair getKeyPair(SessionID paramSessionID) {
		KeyPair pair = keyManager.loadLocalKeyPair(paramSessionID);
		if(pair == null) {
			keyManager.generateLocalKeyPair(paramSessionID);
			return keyManager.loadLocalKeyPair(paramSessionID);
		} else {
			return pair;
		}
	}
	
	private void sendMessage(JID fromuser, JID touser, String body) {
		Message message = createServerMessage(fromuser, touser, body);
		messageRouter.route(message);
	}

	private Message createServerMessage(JID fromuser, JID touser, String body) {
		Message message = new Message();
		message.setTo(touser);
		message.setFrom(fromuser);
		message.setBody(body);
		return message;
	}
}

class DefaultPropertiesStore implements OtrKeyManagerStore {
	private final Properties properties = new Properties();
	private String filepath;

	public DefaultPropertiesStore(String filepath) throws IOException {
		if (filepath == null || filepath.length() < 1)
				throw new IllegalArgumentException();
		this.filepath = filepath;
		properties.clear();

		InputStream in = new BufferedInputStream(new FileInputStream(
						getConfigurationFile()));
		try {
				properties.load(in);
		} finally {
				in.close();
		}
	}

	private File getConfigurationFile() throws IOException {
		File configFile = new File(filepath);
		if (!configFile.exists())
				configFile.createNewFile();
		return configFile;
	}

	public void setProperty(String id, boolean value) {
		loadProperties();
		properties.setProperty(id, "true");
		try {
				this.store();
		} catch (Exception e) {
				e.printStackTrace();
		}
	}
	
	private void loadProperties() {
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(getConfigurationFile()));
			properties.load(in);
			in.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void store() throws FileNotFoundException, IOException {
		OutputStream out = new FileOutputStream(getConfigurationFile());
		properties.store(out, null);
		out.close();
	}

	public void setProperty(String id, byte[] value) {
		loadProperties();
		properties.setProperty(id, new String(Base64.encode(value)));
		try {
				this.store();
		} catch (Exception e) {
				e.printStackTrace();
		}
	}

	public void removeProperty(String id) {
		loadProperties();
		properties.remove(id);
		try {
				this.store();
		} catch (Exception e) {
				e.printStackTrace();
		}
	}

	public byte[] getPropertyBytes(String id) {
		loadProperties();
		String value = properties.getProperty(id);
		if(value!=null)
			return Base64.decode(value);
		else
			return null;
	}

	public boolean getPropertyBoolean(String id, boolean defaultValue) {
		loadProperties();
		try {
				return Boolean.valueOf(properties.get(id).toString());
		} catch (Exception e) {
				return defaultValue;
		}
	}
}

