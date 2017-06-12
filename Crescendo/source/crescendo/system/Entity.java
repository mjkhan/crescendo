package crescendo.system;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import crescendo.system.sql.QuerySupport;
import crescendo.util.StringResource;
import horizon.data.BoundedList;
import horizon.data.Convert;
import horizon.data.DataRecord;
import horizon.database.DBAccess;
import horizon.persistence.AbstractPersistent;
import horizon.persistence.Persistent;
import horizon.system.AbstractObject;

public abstract class Entity extends AbstractPersistent implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String SEARCH = "search";
	public static final String GET = "get";
	public static final String VIEW = "view";
	public static final String NEW = "new";
	public static final String VALIDATE = "validate";
	public static final String CREATE = "create";
	public static final String UPDATE = "update";
	public static final String REMOVE = "remove";
	public static final String CHANGE_STATUS = "change-status";

	public static final String TERMS = "terms";
	public static final String CONDITION = "condition";
	public static final String ORDER = "order";
	public static final String STATUS = "status";
	public static final String ARGS = "args";
	public static final String FETCH = "fetch";
	public static final String START = "start";
	public static final String COUNT = "count";
	public static final String FIELD_NAME = "field";
	public static final String FIELD_VALUE = "field-value";
	public static final String VALID = "field-valid";

	protected transient Config cfg;
	private LinkedHashSet<String> changedFields;

	protected void setModified(String fieldName) {
		if (!State.MODIFIED.equals(setModified())) return;
		setChanged(fieldName);
	}

	protected void setChanged(String fieldName) {
		if (isEmpty(fieldName)) return;

		if (changedFields == null)
			changedFields = new LinkedHashSet<>();
		changedFields.add(fieldName);
	}

	public Set<String> clearChangedFields() {
		if (changedFields == null)
			return Collections.emptySet();
		Set<String> result = changedFields;
		changedFields = null;
		return result;
	}
	@Override
	public void read(DataRecord record) {
		if (cfg != null)
			cfg.setDefaults(record);
		super.read(record);
	}
	@Override
	public void write(DataRecord record) {
		if (cfg != null) {
			cfg.setDefaults(record);
			Validator validator = cfg.validator();
			if (validator != null)
				validator.set(pm.dataAccess()).validate(this);
		}
		super.write(record);
	}

	public static class Config extends Feature.Entry {
		private static final long serialVersionUID = 1L;

		private transient Persistent.Info persistent;
		private transient List<Default> defaults;
		private transient Class<? extends Validator> validator;

		private Persistent.Info persistent() {
			return find(feature(), entry -> {
				Config cfg = (Config)entry;
				return cfg.persistent;
			}, () -> null);
		}

		public Class<? extends Entity> entity() {
			return persistent().klass().asSubclass(Entity.class);
		}

		public <T extends Entity> Class<T> entityAs() {
			return classAs(entity());
		}

		public String table() {
			return persistent().table();
		}

		public DataRecord setDefaults(DataRecord record) {
			List<Default> defs = find(feature(), entry -> {
				Config cfg = (Config)entry;
				return cfg.defaults;
			}, () -> null);
			if (defs != null
			 && State.CREATED.equals(record.state()))
				defs.forEach(def -> def.set(record));
			return record;
		}

		public Validator validator() {
			return find(feature(), entry -> {
				Config cfg = (Config)entry;
				if (cfg.validator == null) return null;

				Validator v = instance(cfg.validator);
				v.cfg = this;
				return v;
			}, () -> Validator.empty);
		}

		private static class Default {
			private String
				fieldName,
				value;

			public void set(DataRecord record) {
				if (!record.hasField(fieldName)) return;
				Object obj = record.getValue(fieldName);
				boolean empty = isEmpty(obj) || record.ignores(fieldName);
				if (!empty) return;

				record.set(fieldName, Convert.toObject(record.getInfo().get(fieldName).fieldClass(), value));
			}
		}

		static class Reader extends ComplexReader {
			private Config cfg;
			@Override
			public Reader setElement(Element element) {
				return Reader.class.cast(super.setElement(element));
			}
			@Override
			public Config value() {
				if (cfg == null) {
					cfg = new Config();
					cfg.setID(id());
					Config.ClassReader classEntry = new Config.ClassReader();
					Class<? extends Persistent> klass = classEntry.setElement(element).as("class");
					if (klass != null)
						cfg.persistent = Persistent.Info.get(klass);
					cfg.defaults = getDefaults();
					cfg.validator = classEntry.setElement(xml.getChild(element, "validator")).as("class");
					readSimpleEntries(cfg);
				}
				return cfg;
			}

			private List<Default> getDefaults() {
				Function<Element, Default> toDefault = child -> {
					 Default def = new Default();
					 def.fieldName = xml.attribute(child, "field");
					 def.value = child.getTextContent();
					 return def;
				};
				return ifEmpty(xml.getChildren(element, "default").stream().map(toDefault).collect(Collectors.toList()),() -> null);
			}
		}
	}

	public static class Factory {
		public static final <T extends Entity> T create(Config cfg) {
			Class<T> klass = cfg.entityAs();
			T t = Config.instance(klass);
			if (t != null)
				t.cfg = cfg;
			return t;
		}

		public static final <T extends Entity> T create(Config cfg, Class<T> def) {
			T t = create(cfg);
			if (t == null && def != null)
				t = Config.instance(def);
			if (t != null)
				t.cfg = cfg;
			return t;
		}

		public static final <T extends Entity> T create(Config cfg, DataRecord record) {
			T t = create(cfg);
			t.read(record);
			return t;
		}

		public static final <T extends Entity> List<T> create(Config cfg, Collection<DataRecord> records) {
			if (isEmpty(records)) return Collections.emptyList();
			Function<DataRecord, T> toEntity = record -> create(cfg, record);
			return records.stream().map(toEntity).collect(Collectors.toCollection(BoundedList::new));
		}
	}

	public static class Validator extends AbstractObject {
		public static final String NAME = "validate";
		public static final String ARGS = "args";
		private static final Validator empty = new Validator();

		protected Config cfg;
		protected DBAccess dataAccess;

		public Validator set(DBAccess dataAccess) {
			this.dataAccess = dataAccess;
			return this;
		}

		public Result validate(String name, Object arg) {
			return validate(name, new NamedObjects().set("value", arg));
		}

		public Result validate(String name, Map<String, Object> args) {
			return Result.VALID;
		}

		public void validate(Entity entity) {
			if (!cfg.entity().isInstance(entity))
				throw new RuntimeException(entity + " is not a(n) " + cfg.entity().getName());
			doValidate(entity);
		}

		protected void doValidate(Entity entity) {}

		private static final String COUNT = "select count(*) a_val from {table}{condition}";

		public int getCount(String table, String condition) {
			String query = COUNT.replace("{table}", table)
								.replace("{condition}", !isEmpty(condition) ? "\nwhere " + condition : "");
			Number number = dataAccess.query("a_val").getValue(query);
			return number.intValue();
		}

		public static class Result implements Serializable {
			private static final long serialVersionUID = 1L;
			public static final Result VALID = new Result().setValid(true);
			public static final String FAILED = "validation-failed";

			public static final Result get(Map<String, Object> map) {
				return Result.class.cast(map.get("val-result"));
			}

			public void setTo(Map<String, Object> map) {
				map.put("val-result", this);
			}

			private boolean valid;
			private String msg;

			public boolean isValid() {
				return valid;
			}

			public Result setValid(boolean valid) {
				this.valid = valid;
				if (valid)
					msg = null;
				return this;
			}

			public String getMessage() {
				return msg;
			}

			public Result setMessage(String msg) {
				this.msg = msg;
				return this;
			}

			public CrescendoException invalid() {
				return CrescendoException.create(ifEmpty(msg, "validation failed")).setId(FAILED);
			}
			@Override
			public String toString() {
				return valid ? "valid" : "invalid";
			}
		}
	}

	public static enum Status implements Codified {
		CREATED("000"),
		ACTIVE("001"),
		INACTIVE("002"),
		REMOVED("998"),
		DELETE("999");

		private final String code;

		private Status(String code) {
			this.code = code;
		}

		public static boolean remove(Status status) {
			return REMOVED.equals(status) || DELETE.equals(status);
		}

		public static Status codeOf(String code) {
			return Codified.codeOf(values(), code);
		}

		public static String displayName(String code, StringResource res) {
			Status status = codeOf(code);
			return status != null ? status.displayName(res) : null;
		}

		public static String condition(String status) {
			if ("none".equals(status)) return "<> '" + DELETE.code() + "'";

			if (isEmpty(status))
				return "not in ('" + REMOVED.code() + "', '" + DELETE.code() + "')";
			else {
				QuerySupport qsupport = QuerySupport.get();
				return qsupport.asIn(qsupport.split(status));
			}
		}

		public static void validate(Status old, Status changed) {
			if (old == null || old.equals(changed)) return;
			if (changed != null) {
				boolean valid = false;
				switch (old) {
				case CREATED: valid = !Status.INACTIVE.equals(changed); break;
				case ACTIVE:
				case INACTIVE:
				case REMOVED: valid = !Status.CREATED.equals(changed); break;
				default: break;
				}
				if (!valid)
					throw new RuntimeException("invalid-status");
			}
		}
		@Override
		public String code() {
			return code;
		}
	};
}