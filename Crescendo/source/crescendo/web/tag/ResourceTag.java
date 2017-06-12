package crescendo.web.tag;

import horizon.web.tag.HorizonTag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import crescendo.util.StringResource;

public class ResourceTag extends HorizonTag {
	private static final long serialVersionUID = 1L;

	static final String attrkey(String name) {
		return "string-resource:" + name;
	}

	static final StringResource find(HttpServletRequest hreq, String name) {
		String key = attrkey(name);
		return StringResource.class.cast(hreq.getAttribute(key));
	}

	private String
		name,
		basepath,
		locale;

	public void setName(String name) {
		this.name = name;
	}

	public void setBasepath(String basepath) {
		this.basepath = basepath;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}
	@Override
	public int doStartTag() throws JspException {
		StringResource res = find(hreq(), name);
		if (res == null)
			hreq().setAttribute(attrkey(name), res = new StringResource().setResource(basepath).setLocale(ifEmpty(locale, () -> hreq().getLocale().toString())));
		return SKIP_BODY;
	}
	@Override
	public void release() {
		name = basepath = locale = null;
		super.release();
	}
}