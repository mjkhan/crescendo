package crescendo.system;

import java.util.function.Function;

import crescendo.util.StringResource;
import horizon.system.Assert;

public interface Codified {
	public String code();

	public default String displayName(StringResource res, Function<Codified, String> keymaker) {
		return res == null ? null :
			res.string(keymaker != null ? keymaker.apply(this) : getClass().getSimpleName() + "." + toString());
	}

	public default String displayName(StringResource res) {
		return displayName(res, null);
	}

	public static <T extends Codified> T codeOf(T[] values, String code) {
		if (Assert.isEmpty(code)) return null;

		for (T t: values)
			if (code.equals(t.code()))
				return t;
		throw new IllegalArgumentException("Invalid code: " + code);
	}
}