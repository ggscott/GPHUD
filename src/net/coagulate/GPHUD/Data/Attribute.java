package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.Data.Audit.OPERATOR;
import net.coagulate.GPHUD.Modules.Experience.GenericXP;
import net.coagulate.GPHUD.Modules.Experience.QuotaedXP;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.*;

/**
 * Contains the data related to an attribute defined for an instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Attribute extends TableRow {

	protected Attribute(final int id) { super(id); }

	// ---------- STATICS ----------

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return An Attribute representation
	 */
	@Nonnull
	public static Attribute get(final int id) {
		return (Attribute) factoryPut("Attribute",id,new Attribute(id));
	}

	/**
	 * Find an attribute in an instance.
	 *
	 * @param instance Instance to look attribute up in.
	 * @param name     Name of attribute to locate
	 *
	 * @return Region object for that region
	 *
	 * @throws UserInputLookupFailureException if the attribute doesn't resolve
	 */
	@Nonnull
	public static Attribute find(@Nonnull final Instance instance,
	                             @Nonnull final String name) {
		try {
			final int id=db().dqinn("select attributeid from attributes where name like ? and instanceid=?",name,instance.getId());
			return get(id);
		}
		catch (@Nonnull final NoDataException e) {
			throw new UserInputLookupFailureException("Unable to find attribute '"+name+"' in instance '"+instance+"'",e);
		}
	}

	/**
	 * Find an attribute that is a group by 'type'.
	 *
	 * @param instance  Instance to look in
	 * @param grouptype Group type (subtype) to look for
	 *
	 * @return The matching Attribute
	 *
	 * @throws UserInputLookupFailureException if there is no matching attribute
	 */
	@Nonnull
	public static Attribute findGroup(@Nonnull final Instance instance,
	                                  @Nonnull final String grouptype) {
		try {
			final int id=db().dqinn("select attributeid from attributes where instanceid=? and attributetype='GROUP' and grouptype=?",instance.getId(),grouptype);
			return get(id);
		}
		catch (@Nonnull final NoDataException e) {
			throw new UserInputLookupFailureException("Unable to find an attribute representing a group of type "+grouptype,e);
		}
	}

	/**
	 * Get the attributes for the instance in this state.
	 *
	 * @param st State, Infers instance
	 *
	 * @return Set of attribute for this instance
	 */
	@Nonnull
	public static Set<Attribute> getAttributes(@Nonnull final State st) { return getAttributes(st.getInstance()); }

	/**
	 * Get the attributes for the instance.
	 *
	 * @param instance The instance to query
	 *
	 * @return Set of attribute for this instance
	 */
	@Nonnull
	public static Set<Attribute> getAttributes(@Nonnull final Instance instance) {
		try { return Cache.<Set<Attribute>>getCache("GPHUD-AttributeSet").get(instance.getId()+""); }
		catch (Cache.CacheMiss e) {
			final Set<Attribute> set = new TreeSet<>();
			for (final ResultsRow r : db().dq("select attributeid from attributes where instanceid=?", instance.getId())) {
				set.add(Attribute.get(r.getInt()));
			}
			return Cache.<Set<Attribute>>getCache("GPHUD-AttributeSet").put(instance.getId()+"",set,60);
		}
	}
	private static void purgeAttributeSetCache(@Nonnull final Instance instance) {
		Cache.<Set<Attribute>>getCache("GPHUD-AttributeSet").purge(instance.getId()+"");
	}

	/**
	 * Find attribute by name
	 *
	 * @param st   State
	 * @param name Attribute name
	 *
	 * @return Attribute or null
	 */
	@Nullable
	public static Attribute findNullable(@Nonnull final State st,
	                                     final String name) {
		final int id=new Attribute(-1).resolveToID(st,name,true);
		if (id==0) { return null; }
		return get(id);
	}

	/**
	 * Convert a text type to an attribute type
	 *
	 * @param type String form of the type
	 *
	 * @return The ATTRIBUTETYPE that corresponds
	 */
	@Nonnull
	public static ATTRIBUTETYPE fromString(@Nonnull final String type) {
		if ("text".equalsIgnoreCase(type)) { return TEXT; }
		if ("integer".equalsIgnoreCase(type)) { return INTEGER; }
		if ("group".equalsIgnoreCase(type)) { return GROUP; }
		if ("pool".equalsIgnoreCase(type)) { return POOL; }
		if ("float".equalsIgnoreCase(type)) { return FLOAT; }
		if ("color".equalsIgnoreCase(type)) { return COLOR; }
		if ("experience".equalsIgnoreCase(type)) { return EXPERIENCE; }
		if ("currency".equalsIgnoreCase(type)) { return CURRENCY; }
		throw new SystemImplementationException("Unhandled type "+type+" to convert to ATTRIBUTETYPE");
	}

	/**
	 * Create a new attribute
	 *
	 * @param instance          Instance to create in
	 * @param name              Name of attribute
	 * @param selfmodify        unpriviledged user self-modify
	 * @param attributetype     "type" of attribute (defined at module level)
	 * @param grouptype         subtype of attribute (see module)
	 * @param usesabilitypoints can be increased by ability points (costs against ability points)
	 * @param required          value must be supplied
	 * @param defaultvalue      default value (where not required attribute)
	 */
	public static void create(@Nonnull final Instance instance,
	                          @Nonnull final String name,
	                          final boolean selfmodify,
	                          @Nonnull final ATTRIBUTETYPE attributetype,
	                          @Nullable final String grouptype,
	                          final boolean usesabilitypoints,
	                          final boolean required,
	                          @Nullable String defaultvalue) {
		if ("".equals(defaultvalue)) { defaultvalue=null; }
		db().d("insert into attributes(instanceid,name,selfmodify,attributetype,grouptype,usesabilitypoints,required,defaultvalue) values(?,?,?,?,?,?,?,?)",
		       instance.getId(),
		       name,
		       selfmodify,
		       toString(attributetype),
		       grouptype,
		       usesabilitypoints,
		       required,
		       defaultvalue
		      );
		purgeAttributeSetCache(instance);
	}

	/**
	 * Create a new attribute
	 *
	 * @param st                State Instance to create in
	 * @param name              Name of attribute
	 * @param selfmodify        unpriviledged user self-modify
	 * @param attributetype     "type" of attribute (defined at module level)
	 * @param grouptype         subtype of attribute (see module)
	 * @param usesabilitypoints can be increased by ability points (costs against ability points)
	 * @param required          value must be supplied
	 * @param defaultvalue      default value (where not required attribute)
	 */
	public static void create(@Nonnull final State st,
	                          @Nonnull final String name,
	                          final boolean selfmodify,
	                          @Nonnull final ATTRIBUTETYPE attributetype,
	                          @Nullable final String grouptype,
	                          final boolean usesabilitypoints,
	                          final boolean required,
	                          @Nullable final String defaultvalue) {
		create(st.getInstance(),name,selfmodify,attributetype,grouptype,usesabilitypoints,required,defaultvalue);
	}

	/**
	 * Convert an ATTRIBUTETYPE back into a string
	 *
	 * @param type ATTRIBUTETYPE
	 *
	 * @return String form
	 */
	@Nonnull
	public static String toString(@Nonnull final ATTRIBUTETYPE type) {
		switch (type) {
			case TEXT:
				return "text";
			case FLOAT:
				return "float";
			case INTEGER:
				return "integer";
			case GROUP:
				return "group";
			case POOL:
				return "pool";
			case COLOR:
				return "color";
			case EXPERIENCE:
				return "experience";
			case CURRENCY:
				return "currency";
		}
		throw new SystemImplementationException("Unhandled attributetype to string mapping for "+type);
	}

	// ----- Internal Statics -----
	// ---------- INSTANCE ----------

	/**
	 * Gets the instance associated with this attribute.
	 *
	 * @return The Instance object
	 */
	@Nonnull
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "attributes";
	}

	@Nonnull
	@Override
	public String getIdColumn() {
		return "attributeid";
	}

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Attribute / State Instance mismatch");
		}
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() { return "attributes"; }

	@Nullable
	@Override
	public String getKVTable() { return null; }

	@Nullable
	@Override
	public String getKVIdField() { return null; }

	@Override
	protected int getNameCacheTime() { return 60*60; } // 1 hour, attributes can NOT be renamed because they create a KV based on the name :P

	/**
	 * Get this attributes ATTRIBUTETYPE
	 *
	 * @return The ATTRIBUTETYPE
	 */
	@Nonnull
	public ATTRIBUTETYPE getType() {
		String type;
		try { type=(String) cacheGet("type"); }
		catch (@Nonnull final CacheMiss ex) {
			type=getString("attributetype");
			cachePut("type",type,getNameCacheTime());
		}
		return fromString(type);
	}

	/**
	 * Get this attribute's subtype, used by groups to define attribute mappings and exclusions.
	 *
	 * @return The sub type of the attribute, may be null.
	 */
	@Nullable
	public String getSubType() {
		return getStringNullable("grouptype");
	}

	/**
	 * Returns wether this attribute uses ability points.
	 *
	 * @return True if it does
	 */
	public boolean usesAbilityPoints() { return getBool("usesabilitypoints"); }

	/**
	 * Return if this attribute is mandatory.
	 *
	 * @return true if this attribute is required
	 */
	public boolean getRequired() { return getBool("required"); }

	/**
	 * Sets the required flag.
	 *
	 * @param required New required flag state.
	 */
	public void setRequired(final boolean required) {
		set("required",required);
	}

	/**
	 * Returns the default value
	 *
	 * @return the default value which may be null
	 */
	@Nullable
	public String getDefaultValue() { return getStringNullable("defaultvalue"); }

	/**
	 * Set the default value for this attribute.
	 *
	 * @param defaultvalue New default value
	 */
	public void setDefaultValue(@Nullable final String defaultvalue) {
		set("defaultvalue",defaultvalue);
	}

	/**
	 * Get the self modify flag.
	 *
	 * @return boolean true if can self modify
	 */
	public boolean getSelfModify() { return getBool("selfmodify"); }

	/**
	 * Set the self modify flag.
	 *
	 * @param selfmodify Character can self modify the attribute
	 */
	public void setSelfModify(final Boolean selfmodify) {
		set("selfmodify",selfmodify);
	}

	/**
	 * Gets the character's current final value for an attribute.
	 *
	 * KVs are passed through the usual getKV mechanism
	 * POOLs and EXPERIENCE are summed pools
	 *
	 * @param st State, infers character
	 *
	 * @return The current value for the character, technically nullable
	 */
	@Nullable
	public String getCharacterValue(@Nonnull final State st) {
		//System.out.println("Attr:"+getName()+" is "+getType()+" of "+getClass().getSimpleName());
		if (isKV()) { return st.getKV("Characters."+getName()).value(); }
		final ATTRIBUTETYPE attrtype=getType();
		if (attrtype==GROUP) {
			if (getSubType()==null) { return null; }
			final CharacterGroup cg=CharacterGroup.getGroup(st,getSubType());
			if (cg==null) { return null; }
			return cg.getName();
		}
		if (attrtype==EXPERIENCE) {
			final GenericXP xp=new GenericXP(getName());
			return CharacterPool.sumPool(st,(xp.getPool(st)))+"";
		}
		if (attrtype==POOL && QuotaedXP.class.isAssignableFrom(getClass())) {
			final QuotaedXP xp=(QuotaedXP) this;
			return CharacterPool.sumPool(st,(xp.getPool(st)))+"";
		}
		if (attrtype==CURRENCY) {
			final Currency currency=Currency.findNullable(st,getName());
			if (currency==null) { return "NotDefined?"; }
			return currency.shortSum(st);
		}
		if (attrtype==POOL) { return "POOL"; }
		throw new SystemImplementationException("Unhandled non KV type "+getType());
	}

	/**
	 * Get additional information about the value this attribute has for a given character
	 *
	 * KVs get the computed path
	 * POOls and EXPERIENCE return quotaed information, if quotaed
	 * GROUPs return nothing
	 *
	 * @param st State infers character
	 *
	 * @return A description of the value
	 */
	@Nonnull
	public String getCharacterValueDescription(@Nonnull final State st) {
		if (isKV()) { return st.getKV("Characters."+getName()).path(); }
		final ATTRIBUTETYPE attrtype=getType();
		if (attrtype==EXPERIENCE) {
			final GenericXP xp=new GenericXP(getName());
			return ("<i>(In last "+xp.periodRoughly(st)+" : "+xp.periodAwarded(st)+")</i>")+(", <i>Next available:"+xp.nextFree(st)+"</i>");
		}
		if (attrtype==POOL && QuotaedXP.class.isAssignableFrom(getClass())) {
			final QuotaedXP xp=(QuotaedXP) this;
			return ("<i>(In last "+xp.periodRoughly(st)+" : "+xp.periodAwarded(st)+")</i>")+(", <i>Next available:"+xp.nextFree(st)+"</i>");
		}
		if (attrtype==POOL) { return "POOL"; }
		if (attrtype==CURRENCY) {
			final Currency currency=Currency.findNullable(st,getName());
			if (currency==null) { return "NotDefined?"; }
			return currency.longSum(st);
		}
		if (attrtype==GROUP) { return ""; }
		throw new SystemImplementationException("Unhandled type "+getType());
	}

	/**
	 * Wether this attribute is represented as a KV.
	 * Group memberships (faction, race) and POOLs including EXPERIENCE are NOT a KV
	 *
	 * @return True if this attribute generates a KV.
	 */
	public boolean isKV() {
		final ATTRIBUTETYPE def=getType();
		if (def==INTEGER || def==FLOAT || def==TEXT || def==COLOR) { return true; }
		if (def==POOL || def==GROUP || def==EXPERIENCE || def==CURRENCY) { return false; }
		throw new SystemImplementationException("Unknown attribute type "+def+" in attribute "+this);
	}

	/**
	 * Gets the KV type that models this attribute.
	 *
	 * @return the KVTYPE
	 *
	 * @throws SystemException If the attribute is not of a KV represented attribute.
	 */
	@Nonnull
	public KV.KVTYPE getKVType() {
		final ATTRIBUTETYPE def=getType();
		if (def==INTEGER) { return KV.KVTYPE.INTEGER; }
		if (def==FLOAT) { return KV.KVTYPE.FLOAT; }
		if (def==TEXT) { return KV.KVTYPE.TEXT; }
		if (def==COLOR) { return KV.KVTYPE.COLOR; }
		throw new SystemImplementationException("Non KV attribute type "+def+" in attribute "+this);
	}

	/**
	 * Gets the default KV value for this attribute.
	 *
	 * @return The default value
	 *
	 * @throws SystemException if this attribute type is not KV backed
	 */
	@Nonnull
	public String getKVDefaultValue() {
		final ATTRIBUTETYPE def=getType();
		if (def==INTEGER) { return "0"; }
		if (def==FLOAT) { return "0"; }
		if (def==TEXT) { return ""; }
		if (def==COLOR) { return "<1,1,1>"; }
		throw new SystemImplementationException("Unhandled KV attribute type "+def+" in attribute "+this);
	}

	/**
	 * Gets the KV hierarchy type for this attribute.
	 *
	 * @return the appropriate KVHIERARCHY
	 *
	 * @throws SystemException If this attribute is not backed by a KV type.
	 */
	@Nonnull
	public KV.KVHIERARCHY getKVHierarchy() {
		final ATTRIBUTETYPE def=getType();
		if (def==INTEGER) { return KV.KVHIERARCHY.CUMULATIVE; }
		if (def==FLOAT) { return KV.KVHIERARCHY.CUMULATIVE; }
		if (def==TEXT) { return KV.KVHIERARCHY.DELEGATING; }
		if (def==COLOR) { return KV.KVHIERARCHY.DELEGATING; }
		throw new SystemImplementationException("Unhandled attribute type "+def+" in attribute "+this);
	}

	/**
	 * Set the uses abilitypoints flag.
	 *
	 * @param usesabilitypoints Flags new value
	 */
	public void setUsesAbilityPoints(final Boolean usesabilitypoints) {
		set("usesabilitypoints",usesabilitypoints);
	}

	/**
	 * Deletes this attribute, and its data.
	 */
	public void delete(final State st) {
		// delete data
		Instance instance=getInstance();
		if (instance!=st.getInstance()) { throw new SystemConsistencyException("State instance / attribute instance mismatch during DELETE of all things"); }
		final ATTRIBUTETYPE type=getType();
		if (type==TEXT || type==FLOAT || type==INTEGER || type==COLOR) { getInstance().wipeKV("Characters."+getName()); }
		if (type==CURRENCY) {
			final Currency c=Currency.findNullable(st,getName());
			if (c!=null) { c.delete(st); }
		}
		d("delete from attributes where attributeid=?",getId());
		purgeAttributeSetCache(instance);
	}

	public boolean readOnly() {
		return false;
	}

	public enum ATTRIBUTETYPE {
		TEXT,
		FLOAT,
		INTEGER,
		GROUP,
		POOL,
		COLOR,
		EXPERIENCE,
		CURRENCY
	}

	public boolean templatable() {
		return getBool("templatable");
	}

	public void templatable(final State st,
	                        final boolean newvalue) {
		if (newvalue==templatable()) { return; }
		Audit.audit(false,st,OPERATOR.AVATAR,null,null,"Set",getName()+"/Templatable",""+templatable(),""+newvalue,"Set templatable to "+newvalue);
		set("templatable",newvalue);
	}
}
