package crescendo.system.file;

import java.io.Serializable;
import java.util.LinkedHashMap;

import org.w3c.dom.Element;

import crescendo.system.Feature;
import crescendo.system.SiteContext;
//import horizon.util.FileSupport;

public class Filebase extends Feature {
	private static final long serialVersionUID = 1L;

	public static final Filebase get(SiteContext sctx) {
		return Filebase.load(sctx.site().filebase());
	}

	public static final Filebase get(Feature feature) {
		return notEmpty(feature.feature("filebase"), "filebase");
	}

	private LinkedHashMap<String, Location> locs = new LinkedHashMap<String, Location>();
	private String
		dirs,
		stashes,
		urlPrefixes,
		stashPrefixes;

	private Location loc(String ext) {
		Location loc = locs.get(ext);
		if (loc == null)
			loc = locs.get(ext.toUpperCase());
		if (loc == null)
			loc = locs.get(ext.toLowerCase());
		if (loc == null)
			loc = locs.get("*");
		if (loc == null)
			throw new NullPointerException("Configuration not found for *." + ext);
		return loc;
	}

	public String getDir(String ext, File.Status status) {
		return loc(ext).getDir(status);
	}

	public String getDir(File file, File.Status status) {
		return getDir(file.getExt(), isEmpty(status) ? file.getStatus() : status);
	}

	public String getUrlPrefix(String ext, File.Status status) {
		return loc(ext).getUrlPrefix(status);
	}

	public String getUrlPrefix(File file) {
		return getUrlPrefix(file.getExt(), file.getStatus());
	}

	public String dirs() {
		return dirs;
	}

	public String stashes() {
		return stashes;
	}

	public String urlPrefixes() {
		return urlPrefixes;
	}

	public String stashPrefixes() {
		return stashPrefixes;
	}
	@Override
	protected void read(Element doc) {
		super.read(doc);
		Reader.xml.getChildren(doc, "file").forEach(element -> {
			Location loc = new Location()
					.setDir(Reader.xml.getChild(element, "dir").getTextContent())
					.setUrlPrefix(Reader.xml.getChild(element, "url-prefix").getTextContent());
			Element e = Reader.xml.getChild(element, "stash");
			if (!isEmpty(e))
				loc.stash = e.getTextContent();
			e = Reader.xml.getChild(element, "delete");
			if (!isEmpty(e))
				loc.delete = e.getTextContent();
			e = Reader.xml.getChild(element, "stash-prefix");
			if (!isEmpty(e))
				loc.stashPrefix = e.getTextContent();
			for (String ext: notEmpty(Reader.xml.attribute(element, "ext"), "ext").split(","))
				locs.put(ext.trim(), loc);
		});
		StringBuilder
			dirBuff = new StringBuilder(),
			stashBuff = new StringBuilder(),
			urlBuff = new StringBuilder(),
			stashPfxBuff = new StringBuilder();
		locs.entrySet().forEach(entry -> {
			String ext = entry.getKey();
			Location loc = entry.getValue();
			if (dirBuff.length() > 0) dirBuff.append("\n            ");
			if (stashBuff.length() > 0) stashBuff.append("\n            ");
			if (urlBuff.length() > 0) urlBuff.append("\n            ");
			if (stashPfxBuff.length() > 0) stashPfxBuff.append("\n            ");
			String prefix = isEmpty(loc.urlPrefix) ? "null" : "'" + loc.urlPrefix + "'",
				   stashPrefix = "'" + loc.stashPrefix() + "'";
			if (ext.equals("*")) {
				dirBuff.append("else '" + loc.dir + "' end");
				stashBuff.append("else '" + loc.stash() + "' end");
				urlBuff.append("else " + prefix + " end");
				stashPfxBuff.append("else " + stashPrefix + " end");
			} else {
				dirBuff.append("when file_name like '%." + ext + "' then '" + loc.dir + "'");
				stashBuff.append("when file_name like '%." + ext + "' then '" + loc.stash() + "'");
				urlBuff.append("when file_name like '%." + ext + "' then " + prefix);
				stashPfxBuff.append("when file_name like '%." + ext + "' then " + stashPrefix);
			}
		});
		dirs = dirBuff.toString();
		stashes = stashBuff.toString();
		urlPrefixes = urlBuff.length() < 1 ? "null" : urlBuff.toString();
		stashPrefixes = stashPfxBuff.length() < 1 ? "null" : stashPfxBuff.toString();
	}

	private static class Location implements Serializable {
		private static final long serialVersionUID = 1L;
		private String
			dir,
			stash,
			delete,
			urlPrefix,
			stashPrefix;

		public String getDir(File.Status status) {
			switch (status) {
			case INACTIVE:
			case REMOVED: return stash();
			case DELETE: return delete();
			default: return dir;
			}
		}

		public Location setDir(String dir) {
			this.dir = notEmpty(dir, "dir").trim();
			return this;
		}

		public String getUrlPrefix(File.Status status) {
			switch (status) {
			case INACTIVE:
			case REMOVED: return stashPrefix();
			case DELETE: throw new IllegalArgumentException(status + " is not allowed.");
			default: return urlPrefix;
			}
		}

		public Location setUrlPrefix(String urlPrefix) {
			if (!isEmpty(urlPrefix)) urlPrefix = urlPrefix.trim();
			this.urlPrefix = urlPrefix;
			return this;
		}

		public String stash() {
			return !isEmpty(stash) ? stash : dir.endsWith("/") ? dir + "stash/" : dir + "/stash";
		}

		public String delete() {
			return !isEmpty(delete) ? delete: dir.endsWith("/") ? dir + "delete/" : dir + "/delete";
		}

		public String stashPrefix() {
			return !isEmpty(stashPrefix) ? stashPrefix : urlPrefix.endsWith("/") ? urlPrefix + "stash/" : urlPrefix + "/stash";
		}
		@Override
		public String toString() {
			return getClass().getName() + "(dir:" + dir + ", url-prefix:" + urlPrefix + ")";
		}
	}
}
