package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Objects extends TableRow {
	public Objects(int id) {
		super(id);
	}

	@Nonnull
	public static Objects get(int id) {
		return (Objects) factoryPut("Objects", id, new Objects(id));
	}

	@Nonnull
	public static Objects find(State st, String uuid) {
		Integer id=GPHUD.getDB().dqi("select id from objects where uuid=?",uuid);
		return new Objects(id);
	}
	@Nullable
	public static Objects findOrNull(State st, String uuid) {
		try { return find(st,uuid); }
		catch (NoDataException e) { return null; }
	}

	public static int getMaxVersion() {
		try {
			return GPHUD.getDB().dqi("select max(version) as maxver from objects");
		} catch (NoDataException e) { return 0; }
	}

	@Nullable
	public ObjectTypes getObjectType() {
		Integer otid= getIntNullable("objecttype");
		if (otid==null) { return null; }
		return ObjectTypes.get(otid);
	}

	@Nonnull
	public static String dumpObjects(@Nonnull State st) {
		Instance instance=st.getInstance();
		StringBuilder r= new StringBuilder("<table border=0><tr><th>UUID</th><th>name</th><th>Owner</th><th>Region</th><th>Location</th><th>Version</th><th>Last RX</th><Th>Object Type</th></tr>");
		for (ResultsRow row:GPHUD.getDB().dq("select objects.*,UNIX_TIMESTAMP()-lastrx as since from objects,regions where objects.regionid=regions.regionid and regions.instanceid=?",instance.getId())) {
			int since=row.getIntNullable("since");
			String bgcol="#dfffdf";
			if (since>16*60) { bgcol="#ffffdf"; }
			if (since>60*60) { bgcol="#ffdfdf"; }
			r.append("<tr bgcolor=").append(bgcol).append(">");
			r.append("<td>").append(row.getStringNullable("uuid")).append("</td>");
			r.append("<td>").append(row.getStringNullable("name")).append("</td>");
			r.append("<td>").append(User.get(row.getIntNullable("owner")).getGPHUDLink()).append("</td>");
			r.append("<td>").append(Region.get(row.getIntNullable("regionid"), true).asHtml(st, true)).append("</td>");
			r.append("<td>").append(row.getStringNullable("location")).append("</td>");
			r.append("<td>").append(row.getIntNullable("version")).append("</td>");
			r.append("<td>").append(UnixTime.duration(since)).append(" ago</td>");
			if (st.hasPermission("Objects.MapObjects")) {
				String objecttype = st.postmap.get(row.getStringNullable("uuid"));
				if (!objecttype.isEmpty()) {
					Integer oldobjecttype=row.getIntNullable("objecttype");
					if (oldobjecttype==null || oldobjecttype!=Integer.parseInt(objecttype)) {
						GPHUD.getDB().d("update objects set objecttype=? where id=?", objecttype, row.getIntNullable("id"));
						Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Set", "ObjectType", "", objecttype, "Set object type for " + row.getStringNullable("name") + " " + row.getStringNullable("uuid"));
						ObjectType ot = ObjectType.materialise(st, ObjectTypes.get(Integer.parseInt(objecttype)));
						JSONObject reconfigurepayload = new JSONObject();
						ot.payload(st, reconfigurepayload);
						new Transmission(Objects.get(row.getIntNullable("id")), reconfigurepayload).start();
					}
				} else { objecttype=row.getStringNullable("objecttype"); }
				r.append("<td>").append(ObjectTypes.getDropDownList(st, row.getStringNullable("uuid")).submitOnChange().setValue(objecttype).asHtml(st, true)).append("</td>"); // editing too, have fun with that.
			} else {
				r.append("<td>").append(row.getStringNullable("objecttype")).append("</td>");
			}
			if (st.hasPermission("Objects.RebootObjects")) {
				r.append("<td><button type=Submit name=reboot value=\"").append(row.getStringNullable("uuid")).append("\">Reboot</button></td>");
			}
			if (st.hasPermission("Objects.ShutdownObjects")) {
				if (row.getStringNullable("uuid").equals(st.postmap.get("shutdown"))) {
					r.append("<td><button type=Submit name=reallyshutdown value=\"").append(row.getStringNullable("uuid")).append("\">CONFIRM SHUTDOWN - THE OBJECT OWNER MUST REBOOT IT TO RESUME SERVICE</button></td>");
				} else {
					r.append("<td><button type=Submit name=shutdown value=\"").append(row.getStringNullable("uuid")).append("\">Shutdown</button></td>");
				}
			}
			r.append("</tr>");
		}
		r.append("</table><br><i>(Objects are expected to check in once every 15 minutes, though if a region is down this may not happen.  Connections are purged after 24 hours inactivity, the object type configuration is not, and can be relinked to a new connection.)</i>");
		return r.toString();
	}

	@Nonnull
	@Override
	public String getIdField() { return "id"; }

	@Override
	public void validate(@Nonnull State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("Object / State Instance mismatch"); }
	}

	@Nonnull
	public Region getRegion() { return Region.get(getIntNullable("regionid"),true); }

	@Nullable
	public String getLocation() { return getString("location"); }
	@Nullable
	public Instance getInstance() {
		return getRegion().getInstance();
	}

	@Nonnull
	@Override
	public String getNameField() { return "name"; }

	@Nonnull
	@Override
	public String getLinkTarget() { return "/GPHUD/configuration/objects/object/"+getId(); }

	@Override
	protected int getNameCacheTime() { return 600; }

	@Nullable
	@Override
	public String getKVTable() {
		return null;
	}

	@Nullable
	@Override
	public String getKVIdField() {
		return null;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "objects";
	}

	@Nonnull
	public static Objects connect(State st, String uuid, String name, @Nonnull Region region, @Nonnull User owner, String location, String url, int version) {
		Objects object=findOrNull(st,uuid);
		if (object==null) {
			GPHUD.getDB().d("insert into objects(uuid,name,regionid,owner,location,lastrx,url,version) values(?,?,?,?,?,?,?,?)",uuid,name,region.getId(),owner.getId(),location, UnixTime.getUnixTime(),url,version);
			object=findOrNull(st,uuid);
			if (object==null) { throw new SystemException("Object not found for uuid "+uuid+" after creating it"); }
		} else {
			GPHUD.getDB().d("update objects set name=?,regionid=?,owner=?,location=?,lastrx=?,url=?,version=? where id=?",name,region.getId(),owner.getId(),location,UnixTime.getUnixTime(),url,version,object.getId());
		}
		return object;
	}

	@Nonnull
	public String toString() { return "Object#"+getId()+"='"+getName()+"'@"+getRegion().toString()+"/"+getLocation();}

	@Nullable
	public String getURL() {
		return getString("url");
	}

	public void updateRX() {
		Integer lastrx= getIntNullable("lastrx");
		if (lastrx==null) { lastrx=0; }
		int diff=UnixTime.getUnixTime()-lastrx;
		if (diff>60) { set("lastrx",UnixTime.getUnixTime()); }
	}
}
