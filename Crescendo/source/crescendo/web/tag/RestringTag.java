package crescendo.web.tag;

import horizon.web.tag.HorizonTag;

import javax.servlet.jsp.JspException;

import crescendo.util.StringResource;

public class RestringTag extends HorizonTag {
	private static final long serialVersionUID = 1L;
	private String
		res,
		string,
		format;
	private String[] args;

	public void setRes(String res) {
		this.res = res;
	}

	public void setString(String string) {
		this.string = string;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public void setArgs(String args) {
		this.args = isEmpty(args) ? null : args.split(",");
	}
	@Override
	public int doStartTag() throws JspException {
		try {
			StringResource resource = ResourceTag.find(hreq(), res);
			if (resource == null)
				throw new NullPointerException(StringResource.class.getName() + " not found referenced as " + res);
			if (!isEmpty(string))
				out().print(resource.string(string));
			if (!isEmpty(format))
				out().print(resource.format(format, args));
			return SKIP_BODY;
		} catch (Exception e) {
			throw jspException(e);
		}
	}
	@Override
	public void release() {
		args = null;
		res = string = format = null;
		super.release();
	}
}