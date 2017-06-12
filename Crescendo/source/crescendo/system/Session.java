package crescendo.system;

import horizon.system.AbstractObject;

import java.io.Serializable;
import java.sql.Timestamp;

import crescendo.util.TimeSupport;

public class Session extends AbstractObject implements Serializable {
	public static enum Status {
		ACTIVE,
		TIMEOUT
	}

	private static final long serialVersionUID = 1L;
	public static final String OBJ = "session";
	public static final String TOKEN = OBJ + "-token";
	public static final String PERMANENT = "permanent-" + OBJ;

	private String id;
	private Timestamp lastAccess;
	private int expiration;

	private Status status;

	public String id() {
		return id;
	}

	public Session setId(String id) {
		this.id = id;
		return this;
	}

	public Timestamp lastAccess() {
		return lastAccess;
	}

	public Session updateAccessTime() {
		lastAccess = TimeSupport.now();
		status = Status.ACTIVE;
		return this;
	}

	public boolean isPermanent() {
		return expiration < 1;
	}

	public boolean isExpired() {
		return !isPermanent()
			 && System.currentTimeMillis() > lastAccess.getTime() + (expiration * 60 * 1000);
	}

	public Session setExpiration(int expiration) {
		this.expiration = expiration;
		return this;
	}

	public Status status() {
		return status;
	}

	public Session timeout() {
		id = null;
		status = Status.TIMEOUT;
		return this;
	}

	public static final Session get(Client client) {
		return !isEmpty(client) ? Session.class.cast(client.get(OBJ)) : null;
	}

	public Session setTo(Client client) {
		if (client != null)
			client.set(OBJ, this);
		return this;
	}

	public static final Session removeFrom(Client client) {
		Session session = get(client);
		if (client != null)
			client.set(OBJ, null);
		return session;
	}

	public static Session parse(String s) {
		String[] tokens = ifEmpty(s, "").split(";");
		int length = tokens.length;
		if (length != 3) return null;

		Session session = new Session();
		for (int i = 0; i < length; ++i)
		switch (i) {
		case 0: session.setId(tokens[i]); break;
		case 1: session.lastAccess = TimeSupport.timeOf(tokens[i]); break;
		case 2: session.expiration = Integer.parseInt(tokens[i]); break;
		}
		return session;
	}

	public String string() {
		return String.format("%s;%s;%d", id, lastAccess.toString(), expiration);
	}
	@Override
	public String toString() {
		return getClass().getName() + "(" + string() + ")";
	}
/*
	public static void main(String[] args) {
		Session s = new Session().setExpiration(0);
		s.lastAccess = TimeSupport.timeOf("2015-05-03 18:25:01");
		System.out.println(s.isExpired());
	}
*/
}