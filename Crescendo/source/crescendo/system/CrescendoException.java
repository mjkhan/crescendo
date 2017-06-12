package crescendo.system;

import horizon.system.Assert;

public class CrescendoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public static final CrescendoException create(Throwable t) {
		if (t instanceof CrescendoException)
			return CrescendoException.class.cast(t);

		CrescendoException ce = new CrescendoException();
		if (t != null)
			ce.initCause(Assert.rootCause(t));
		return ce;
	}

	public static final CrescendoException create(String msg) {
		return new CrescendoException().setMessage(msg);
	}

	private String
		id,
		msg;
	private NamedObjects objs;

	public boolean isIdentified() {
		return !Assert.isEmpty(getId());
	}

	public String getId() {
		return id;
	}

	public CrescendoException setId(String id) {
		this.id = id;
		return this;
	}
	@Override
	public String getMessage() {
		return Assert.ifEmpty(msg, () -> {
			Throwable t = getCause();
			return t != null ? t.getMessage() : String.format("%s(%s)", getClass().getName(), id);
		});
	}

	public CrescendoException setMessage(String msg) {
		this.msg = msg;
		return this;
	}

	public NamedObjects objects() {
		return objs != null ? objs : (objs = new NamedObjects());
	}
	@Override
	public String toString() {
		return String.format("%s(%s)", getClass().getName(), isIdentified() ? "\"" + getId() + "\"" : getMessage());
	}
}