package crescendo.bean;

import crescendo.system.Job;

public class CrescendoJob extends CrescendoMessageBean {
	@Override
	protected void consume(Object obj) throws Exception {
		if (obj instanceof Iterable)
			process(Iterable.class.cast(obj));
		else if (obj instanceof Object[])
			process(Object[].class.cast(obj));
		else
			process(obj);
	}

	private void process(Iterable<?> objs) throws Exception {
		if (objs == null) return;
		for (Object job: objs)
			process(job);
	}

	private void process(Object... objs) throws Exception {
		if (objs == null) return;
		for (Object job: objs)
			process(job);
	}

	private void process(Object obj) throws Exception {
		if (!(obj instanceof Job)) {
			log().debug(() -> obj + " ignored, not a " + Job.class.getName());
			return;
		}

		Job job = Job.class.cast(obj);
		try {
			job.execute();
		} catch (Exception e) {
			throw e;
		}
	}
}