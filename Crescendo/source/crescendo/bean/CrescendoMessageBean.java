package crescendo.bean;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

public abstract class CrescendoMessageBean extends CrescendoBean implements MessageListener {
	@Override
	public void onMessage(Message msg) {
		if (!(msg instanceof ObjectMessage)) {
			log().warn(msg + " ignored, not an ObjectMessage.");
			return;
		}
		ObjectMessage omsg = ObjectMessage.class.cast(msg);
		try {
			consume(omsg.getObject());
		} catch (Exception e) {
			log().error(e);
		}
	}

	protected abstract void consume(Object obj) throws Exception;
}