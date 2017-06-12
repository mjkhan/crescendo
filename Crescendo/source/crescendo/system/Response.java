package crescendo.system;

import java.util.Map;

public class Response extends NamedObjects {
	private static final long serialVersionUID = 1L;
	@Override
	public Response set(String name, Object obj) {
		super.set(name, obj);
		return this;
	}
	@Override
	public Response setAll(Map<String, ?> objs) {
		super.setAll(objs);
		return this;
	}

	private static final String PERFORMED = "actionPerformed";

	public Response set(Event evt) {
		if (evt != null) {
			set(PERFORMED, Boolean.valueOf(evt.isFired()));
			evt.setTo(this);
		}
		return setAll(evt);
	}

	private static final String EXCEPTION = "exception";

	public <E extends Throwable> E exception() {
		return (E)get(EXCEPTION);
	}

	public boolean isFailed() {
		return !isEmpty(exception());
	}

	public Response set(Throwable t) {
		boolean failed = !isEmpty(t);
		if (failed)
			set(PERFORMED, Boolean.FALSE);
		return set(EXCEPTION, t);
	}
}