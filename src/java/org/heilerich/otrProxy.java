package org.heilerich;

import java.io.File;
import java.util.regex.PatternSyntaxException;
import java.util.HashMap;

import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.EmailService;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

/**
 * otrProxy plugin.
 * 
 * @author Heilerich
 */
public class otrProxy implements Plugin, PacketInterceptor {

	private static final Logger Log = LoggerFactory.getLogger(otrProxy.class);

    /**
     * Props - variables
     */
    public static final String ENABLED_PROPERTY = "plugin.otrproxy.enabled";
	private boolean pluginEnabled;

    /**
     * the hook into the inteceptor chain
     */
    private InterceptorManager interceptorManager;

    /**
     * used to send system notifications
     */
    private MessageRouter messageRouter;
	
	 /**
     * used to check users
     */
    private XMPPServer srvr;
	
	/**
     * contains otrUserRepresentations identified by the users bare JID
     */
    private HashMap<String,otrUserRepresentation> UserRepr;

	
	/**
     * constructor
     */
    public otrProxy() {
        interceptorManager = InterceptorManager.getInstance();
        messageRouter = XMPPServer.getInstance().getMessageRouter();
		UserRepr = new HashMap<String,otrUserRepresentation>();
		srvr = XMPPServer.getInstance();
    }

    /**
     * Props functions
     */
    public void reset() {
        setPluginEnabled(true);
    }
    
    public boolean isPluginEnabled() {
        return pluginEnabled;
    }
    
    public void setPluginEnabled(boolean enabled) {
        pluginEnabled = enabled;
        JiveGlobals.setProperty(ENABLED_PROPERTY, enabled ? "true"
                : "false");
    }

	/**
     * init
     */
    public void initializePlugin(PluginManager pManager, File pluginDirectory) {
		Log.info("init follows");
        // configure this plugin
        initProxy();
		Log.info("init finished, adding interceptor");
        // register with interceptor manager
        interceptorManager.addInterceptor(this);
		Log.info("interceptor added");
    }

    private void initProxy() {
        // default to true
        pluginEnabled = JiveGlobals.getBooleanProperty(
                ENABLED_PROPERTY, true);
    }

    public void destroyPlugin() {
        interceptorManager.removeInterceptor(this);
    }

    public void interceptPacket(Packet packet, Session session, boolean read,
            boolean processed) throws PacketRejectedException {
			
		if(!pluginEnabled)
			return;
        if (isValidTargetPacket(packet, read, processed)) {
            Packet original = packet;

			if(packet instanceof Message) {
				Message msg = (Message)packet;
				if(msg.getType() == Message.Type.chat) {
					if(msg.getBody() != null) {
						handleMessage(msg);
					}
				}
			} else {
				Log.info("validation error");
			}
        } else {

		}
    }
	
	private void handleMessage(Message msg) {
		if(srvr.isLocal(msg.getFrom())) {
			if(srvr.isLocal(msg.getTo())) {

			} else {
				if(checkCommands(msg))
					return;
				handleOutgoingMessage(msg);
			}
		} else {
			handleIncomingMessage(msg);
		}
	}
	
	private void handleOutgoingMessage(Message msg) {
		JID localuser = msg.getFrom();
		JID remoteuser = msg.getTo();
		String transformed = getUserRepr(localuser).transformSend(remoteuser,msg.getBody());
		msg.setBody(transformed);
	}
	
	private void handleIncomingMessage(Message msg) {
		JID localuser = msg.getTo();
		JID remoteuser = msg.getFrom();
		String transformed = getUserRepr(localuser).transformRecv(remoteuser,msg.getBody());
		msg.setBody(transformed);
	}
	
	private boolean checkCommands(Message msg) {
		JID localuser = msg.getFrom();
		JID remoteuser = msg.getTo();
		if(msg.getBody().startsWith("!!status")) {
			String sstatus = getUserRepr(localuser).getSessionStatus(remoteuser);
			boolean verified = getUserRepr(localuser).isVerified(remoteuser);
			msg.setBody("[otr-server] session is " + sstatus + ((sstatus=="encrypted") ? (". Fingerprint is " + ((verified) ? "verified" : "unverified")) : ""));
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith("!!start")) {
			msg.setBody("[otr-server] starting session");
			getUserRepr(localuser).startSession(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith("!!stop")) {
			msg.setBody("[otr-server] stopping session");
			getUserRepr(localuser).endSession(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith("!!refresh")) {
			msg.setBody("[otr-server] refreshing session");
			getUserRepr(localuser).refreshSession(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith("!!myfingerprint")) {
			String fprint = getUserRepr(localuser).getLocalFingerprint(remoteuser);
			msg.setBody("[otr-server] Your Fingerprint is " + fprint);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith("!!fingerprint")) {
			String fprint = getUserRepr(localuser).getRemoteFingerprint(remoteuser);
			msg.setBody("[otr-server] The Fingerprint is " + fprint);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith("!!verify")) {
			msg.setBody("[otr-server] verify fingerprint");
			getUserRepr(localuser).verify(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith("!!unverify")) {
			msg.setBody("[otr-server] unverify fingerprint");
			getUserRepr(localuser).unverify(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith("!!help")) {
			String cmdlist = "!!start - start otr session\n";
			cmdlist += "!!stop - end otr session\n";
			cmdlist += "!!status - show encrpytion and verification status of the session\n";
			cmdlist += "!!refresh - refresh otr session\n";
			cmdlist += "!!myfingerprint - show your own public key fingerprint\n";
			cmdlist += "!!fingerprint - show your buddy's public key fingerprint for verification\n";
			cmdlist += "!!verify - mark your buddy's public key fingerprint as verified\n";
			cmdlist += "!!unverfiy - mark your buddy's public key fingerprint as unverified\n";
			msg.setBody("[otr-server]\n" + cmdlist);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		return false;
	}
	
	private otrUserRepresentation getUserRepr(JID inuser) {
		otrUserRepresentation repr = UserRepr.get(inuser.toBareJID());
		if(repr!=null) {
			return repr;
		} else {
			UserRepr.put(inuser.toBareJID(),new otrUserRepresentation(inuser));
			repr = UserRepr.get(inuser.toBareJID());
			return repr;
		}
	}

    private boolean isValidTargetPacket(Packet packet, boolean read,
            boolean processed) {
			boolean typechat = false;
			if(packet instanceof Message) {
				Message msg = (Message)packet;
				if(msg.getType() == Message.Type.chat) {
					typechat = true;
				}
			}
        return !processed
                && read
                && packet instanceof Message
				&& typechat;
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