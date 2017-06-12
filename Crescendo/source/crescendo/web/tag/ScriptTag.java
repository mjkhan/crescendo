package crescendo.web.tag;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.servlet.jsp.JspException;

import horizon.web.tag.HorizonTag;

public class ScriptTag extends HorizonTag {
	private static final long serialVersionUID = 1L;
	private String
		src,
		write;

	public void setSrc(String src) {
		this.src = src;
	}

	public void setWrite(String write) {
		this.write = write;
	}
	@Override
	public int doStartTag() throws JspException {
		try {
			if (!isEmpty(write))
				return doWrite();
			if (!isEmpty(src))
				return setSrc();
			return EVAL_BODY_BUFFERED;
		} catch (Exception e) {
			throw jspException(e);
		}
	}
	@Override
	public int doAfterBody() throws JspException {
		if (isEmpty(write) && isEmpty(src))
			setFunc();
		return SKIP_BODY;
	}

	private static final String
		SRC = "<script type=\"text/javascript\" src=\"{src}\"></script>",
		OPEN = "<script type=\"text/javascript\">",
		CLOSE = "</script>",
		SCRIPT_SRC = "crsnd-script-src",
		SCRIPT = "crsnd-script";

	private int doWrite() throws Exception {
		PrintWriter writer = hresp().getWriter();
		ArrayList<String> scripts = (ArrayList<String>)hreq().getAttribute(SCRIPT_SRC);
		if (!isEmpty(scripts))
			writer.println(scripts.stream().map(src -> SRC.replace("{src}", src)).collect(Collectors.joining("\n")));

		scripts = (ArrayList<String>)hreq().getAttribute(SCRIPT);
		if (!isEmpty(scripts)) {
			writer.println(OPEN);
			writer.println(String.join("\n", scripts));
			writer.println(CLOSE);
		}
		return SKIP_BODY;
	}

	private int setSrc() {
		ArrayList<String> srcs = (ArrayList<String>)hreq().getAttribute(SCRIPT_SRC);
		if (srcs == null)
			hreq().setAttribute(SCRIPT_SRC, srcs = new ArrayList<>());
		if (!srcs.contains(src))
			srcs.add(src);
		return SKIP_BODY;
	}

	private void setFunc() {
		String content = bodyContent.getString();
		if (!isEmpty(content)) {
			ArrayList<String> scripts = (ArrayList<String>)hreq().getAttribute(SCRIPT);
			if (scripts == null)
				hreq().setAttribute(SCRIPT, scripts = new ArrayList<>());
			scripts.add(content);
		}
	}
	@Override
	public void release() {
		src = write = null;
		super.release();
	}
}