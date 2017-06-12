package crescendo.system;

import horizon.jms.QueueAccess;

import java.util.Map;

public abstract class Job extends Effect.Implementation {
	private static final long serialVersionUID = 1L;
	private static final Sender sender;
	static {
		Class<Sender> klass = Crescendo.get().klass("job-sender");
		sender = Crescendo.instance(klass);
	}

	public static final void send(Iterable<Job> jobs) {
		if (jobs == null) return;

		Support.splitByConfig(jobs).forEach(Job::send);
	}

	public static final void send(String queueConfig, Iterable<Job> jobs) {
		if (sender == null)
			QueueAccess.send(queueConfig, jobs);
		else
			sender.send(queueConfig, jobs);
	}
	@Override
	public Job set(String name, Object value) {
		super.set(name, value);
		return this;
	}
	@Override
	public Job setAll(Map<String, ? extends Object> objs) {
		super.setAll(objs);
		return this;
	}

	public abstract void execute();

	public static interface Sender {
		public void send(String configName, Iterable<Job> jobs);
	}
}