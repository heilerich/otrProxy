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
	
	public static final String CMD_STATUS_PROPERTY = "plugin.otrproxy.cmd.status";
	private String cmdStatus;
	
	public static final String CMD_START_PROPERTY = "plugin.otrproxy.cmd.start";
	private String cmdStart;
	
	public static final String CMD_STOP_PROPERTY = "plugin.otrproxy.cmd.stop";
	private String cmdStop;
	
	public static final String CMD_REFRESH_PROPERTY = "plugin.otrproxy.cmd.refresh";
	private String cmdRefresh;
	
	public static final String CMD_MYFP_PROPERTY = "plugin.otrproxy.cmd.myfp";
	private String cmdMyfp;
	
	public static final String CMD_FP_PROPERTY = "plugin.otrproxy.cmd.fp";
	private String cmdFp;
	
	public static final String CMD_VERIFY_PROPERTY = "plugin.otrproxy.cmd.verify";
	private String cmdVerify;
	
	public static final String CMD_UNVERIFY_PROPERTY = "plugin.otrproxy.cmd.unverify";
	private String cmdUnverify;
	
	public static final String CMD_HELP_PROPERTY = "plugin.otrproxy.cmd.help";
	private String cmdHelp;
	
	public static final String CMD_KEYFILE_PROPERTY = "plugin.otrproxy.keyfile";
	private String propKeyfile;

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
		setCmdStatus("!!status");
		setCmdStart("!!start");
		setCmdStop("!!stop");
		setCmdRefresh("!!refresh");
		setCmdMyfp("!!myfingerprint");
		setCmdFp("!!fingerprint");
		setCmdVerify("!!verify");
		setCmdUnverify("!!unverify");
		setCmdHelp("!!help");
		setPropKeyfile("key-store.dat");
    }
    
    public boolean isPluginEnabled() {
        return pluginEnabled;
    }
	
	public String getCmdStatus() {
        return cmdStatus;
    }
	
	public String getCmdStart() {
        return cmdStart;
    }
	
	public String getCmdStop() {
        return cmdStop;
    }
	
	public String getCmdRefresh() {
        return cmdRefresh;
    }
	
	public String getCmdMyfp() {
        return cmdMyfp;
    }
	
	public String getCmdFp() {
        return cmdFp;
    }
	
	public String getCmdVerify() {
        return cmdVerify;
    }
	
	public String getCmdUnverify() {
        return cmdUnverify;
    }
	
	public String getCmdHelp() {
        return cmdHelp;
    }
	
	public String getPropKeyfile() {
        return propKeyfile;
    }
    
    public void setPluginEnabled(boolean enabled) {
        pluginEnabled = enabled;
        JiveGlobals.setProperty(ENABLED_PROPERTY, enabled ? "true" : "false");
    }
	
	public void setCmdStatus(String cmd) {
        cmdStatus = cmd;
        JiveGlobals.setProperty(CMD_STATUS_PROPERTY, cmd);
    }
	
	public void setCmdStart(String cmd) {
        cmdStart = cmd;
        JiveGlobals.setProperty(CMD_START_PROPERTY, cmd);
    }
	
	public void setCmdStop(String cmd) {
        cmdStop = cmd;
        JiveGlobals.setProperty(CMD_STOP_PROPERTY, cmd);
    }
	
	public void setCmdRefresh(String cmd) {
        cmdRefresh = cmd;
        JiveGlobals.setProperty(CMD_REFRESH_PROPERTY, cmd);
    }
	
	public void setCmdMyfp(String cmd) {
        cmdMyfp = cmd;
        JiveGlobals.setProperty(CMD_MYFP_PROPERTY, cmd);
    }
	
	public void setCmdFp(String cmd) {
        cmdFp = cmd;
        JiveGlobals.setProperty(CMD_FP_PROPERTY, cmd);
    }
	
	public void setCmdVerify(String cmd) {
        cmdVerify = cmd;
        JiveGlobals.setProperty(CMD_VERIFY_PROPERTY, cmd);
    }
	
	public void setCmdUnverify(String cmd) {
        cmdUnverify = cmd;
        JiveGlobals.setProperty(CMD_UNVERIFY_PROPERTY, cmd);
    }
	
	public void setCmdHelp(String cmd) {
        cmdHelp = cmd;
        JiveGlobals.setProperty(CMD_HELP_PROPERTY, cmd);
    }
	
	public void setPropKeyfile(String cmd) {
        propKeyfile = cmd;
        JiveGlobals.setProperty(CMD_KEYFILE_PROPERTY, cmd);
    }

	/**
     * init
     */
    public void initializePlugin(PluginManager pManager, File pluginDirectory) {
		Log.info("init follows");
        initProxy();
		Log.info("init finished, adding interceptor");
        interceptorManager.addInterceptor(this);
		Log.info("interceptor added");
    }

    private void initProxy() {
        pluginEnabled = JiveGlobals.getBooleanProperty(ENABLED_PROPERTY, true);
		cmdStatus = JiveGlobals.getProperty(CMD_STATUS_PROPERTY, "!!status");
		cmdStart = JiveGlobals.getProperty(CMD_START_PROPERTY, "!!start");
		cmdStop = JiveGlobals.getProperty(CMD_STOP_PROPERTY, "!!stop");
		cmdRefresh = JiveGlobals.getProperty(CMD_REFRESH_PROPERTY, "!!refresh");
		cmdMyfp = JiveGlobals.getProperty(CMD_MYFP_PROPERTY, "!!myfingerprint");
		cmdFp = JiveGlobals.getProperty(CMD_FP_PROPERTY, "!!fingerprint");
		cmdVerify = JiveGlobals.getProperty(CMD_VERIFY_PROPERTY, "!!verify");
		cmdUnverify = JiveGlobals.getProperty(CMD_UNVERIFY_PROPERTY, "!!unverify");
		cmdHelp = JiveGlobals.getProperty(CMD_HELP_PROPERTY, "!!help");
		propKeyfile = JiveGlobals.getProperty(CMD_KEYFILE_PROPERTY, "key-store.dat");
    }

    public void destroyPlugin() {
        interceptorManager.removeInterceptor(this);
    }

    public void interceptPacket(Packet packet, Session session, boolean read, boolean processed) throws PacketRejectedException {
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
		
		if(msg.getBody().startsWith(cmdStatus)) {
			String sstatus = getUserRepr(localuser).getSessionStatus(remoteuser);
			boolean verified = getUserRepr(localuser).isVerified(remoteuser);
			msg.setBody("[otr-server] session is " + sstatus + ((sstatus=="encrypted") ? (". Fingerprint is " + ((verified) ? "verified" : "unverified")) : ""));
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith(cmdStart)) {
			msg.setBody("[otr-server] starting session");
			getUserRepr(localuser).startSession(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith(cmdStop)) {
			msg.setBody("[otr-server] stopping session");
			getUserRepr(localuser).endSession(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith(cmdRefresh)) {
			msg.setBody("[otr-server] refreshing session");
			getUserRepr(localuser).refreshSession(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith(cmdMyfp)) {
			String fprint = getUserRepr(localuser).getLocalFingerprint(remoteuser);
			msg.setBody("[otr-server] Your Fingerprint is " + fprint);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith(cmdFp)) {
			String fprint = getUserRepr(localuser).getRemoteFingerprint(remoteuser);
			msg.setBody("[otr-server] The Fingerprint is " + fprint);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith(cmdVerify)) {
			msg.setBody("[otr-server] verify fingerprint");
			getUserRepr(localuser).verify(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith(cmdUnverify)) {
			msg.setBody("[otr-server] unverify fingerprint");
			getUserRepr(localuser).unverify(remoteuser);
			msg.setTo(localuser);
			msg.setFrom(remoteuser);
			return true;
		}
		if(msg.getBody().startsWith(cmdHelp)) {
			String cmdlist = cmdStart + " - start otr session\n";
			cmdlist += cmdStop + " - end otr session\n";
			cmdlist += cmdStatus + " - show encrpytion and verification status of the session\n";
			cmdlist += cmdRefresh + " - refresh otr session\n";
			cmdlist += cmdMyfp + " - show your own public key fingerprint\n";
			cmdlist += cmdFp + " - show your buddy's public key fingerprint for verification\n";
			cmdlist += cmdVerify + " - mark your buddy's public key fingerprint as verified\n";
			cmdlist += cmdUnverify + " - mark your buddy's public key fingerprint as unverified\n";
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
			UserRepr.put(inuser.toBareJID(),new otrUserRepresentation(inuser, propKeyfile));
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