package crescendo.web.tag;

import javax.servlet.jsp.JspException;

import crescendo.system.Codified;
import crescendo.util.StringResource;
import horizon.data.FieldAware;
import horizon.web.tag.DataTag;

public class CodeStringTag extends DataTag {
	private static final long serialVersionUID = 1L;

	private String
		res,
		code;
	private Class<? extends Codified> mapper;

	public void setRes(String res) {
		this.res = res;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setMapper(Class<? extends Codified> mapper) {
		this.mapper = mapper;
	}
	@Override
	public int doStartTag() throws JspException {
		try {
			String value = null;
			if (!isEmpty(code))
				value = codeValue(code);
			else if (!isEmpty(field))
				value = codeValue(fieldValue());
			if (!isEmpty(value))
				out().print(value);//TODO:getIfEmpty
			return SKIP_BODY;
		} catch (Exception e) {
			throw jspException(e);
		}
	}

	private String codeValue(String code) {
		if (isEmpty(code)) return null;
		StringResource resource = ResourceTag.find(hreq(), res);
		Codified codified = Codified.codeOf(mapper.getEnumConstants(), code);
		return codified.displayName(resource);
	}

	private String fieldValue() throws Exception {
		FieldAware fieldAware = dataAsFieldAware();
		return fieldAware != null ? fieldAware.string(field) : null;
	}
	@Override
	public void release() {
		mapper = null;
		res = code = null;
		super.release();
	}
}