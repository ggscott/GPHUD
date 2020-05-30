package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputInvalidChoiceException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Audit.OPERATOR;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Menus entry - a name, description and complex JSONobject that wraps the button name, and commands.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Menu extends TableRow {

	protected Menu(final int id) { super(id); }

	// ---------- STATICS ----------

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return An Avatar representation
	 */
	@Nonnull
	public static Menu get(final int id) {
		return (Menu) factoryPut("Menus",id,new Menu(id));
	}

	/**
	 * Get a list of menus and their ID for an instance.
	 *
	 * @param st Infers instance
	 *
	 * @return Map of String menu name to Integer menu ID for this instance.
	 */
	@Nonnull
	public static Map<String,Integer> getMenusMap(@Nonnull final State st) {
		final Map<String,Integer> aliases=new TreeMap<>();
		for (final ResultsRow r: db().dq("select name,menuid from menus where instanceid=?",st.getInstance().getId())) {
			aliases.put(r.getStringNullable("name"),r.getIntNullable("menuid"));
		}
		return aliases;
	}

	/**
	 * Load instance menu by name
	 *
	 * @param st   State (infers instance)
	 * @param name Name of the menu to load
	 *
	 * @return Menus object
	 */
	@Nullable
	public static Menu getMenuNullable(@Nonnull final State st,
	                                   @Nonnull final String name) {
		try {
			final int id=db().dqinn("select menuid from menus where instanceid=? and name like ?",st.getInstance().getId(),name);
			return get(id);
		}
		catch (@Nonnull final NoDataException e) { return null; }
	}

	@Nonnull
	public static Menu getMenu(@Nonnull final State st,
	                           @Nonnull final String name) {
		final Menu ret=getMenuNullable(st,name);
		if (ret==null) { throw new UserInputLookupFailureException("No menu called "+name+" is found"); }
		return ret;
	}

	/**
	 * Create a new menu,by name and description, with the given json data blob
	 *
	 * @param st          State (infers instance)
	 * @param name        Name of the new menu
	 * @param description Description of the new menu
	 * @param template    JSONObject template for the new menu (belongs to Menus module)
	 *
	 * @return the new Menus object
	 *
	 * @throws UserException If the name is invalid or duplicated.
	 */
	@Nonnull
	public static Menu create(@Nonnull final State st,
	                          @Nonnull final String name,
	                          @Nonnull final String description,
	                          @Nonnull final JSONObject template) {
		if (getMenuNullable(st,name)!=null) {
			throw new UserInputDuplicateValueException("Menu "+name+" already exists");
		}
		if (name.matches(".*[^A-Za-z0-9-=_,].*")) {
			throw new UserInputValidationParseException("Menu name must not contain spaces, and mostly only allow A-Z a-z 0-9 - + _ ,");
		}
		db().d("insert into menus(instanceid,name,description,json) values(?,?,?,?)",st.getInstance().getId(),name,description,template.toString());
		final Menu newalias=getMenuNullable(st,name);
		if (newalias==null) {
			throw new SystemConsistencyException("Failed to create alias "+name+" in instance id "+st.getInstance().getId()+", created but not found?");
		}
		return newalias;
	}

	/**
	 * Load all the menus for an instance
	 *
	 * @param st State, infers instance
	 *
	 * @return Map of Name to JSONPayloads for all menus in this instance.
	 */
	@Nonnull
	public static Map<String,JSONObject> getTemplates(@Nonnull final State st) {
		final Map<String,JSONObject> aliases=new TreeMap<>();
		for (final ResultsRow r: db().dq("select name,description,json from menus where instanceid=?",st.getInstance().getId())) {
			aliases.put(r.getString("name"),new JSONObject(r.getString("json")));
		}
		return aliases;
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getTableName() {
		return "menus";
	}

	@Nonnull
	@Override
	public String getIdColumn() {
		return "menuid";
	}

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Menus / State Instance mismatch");
		}
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/configuration/menus/"+getId();
	}

	@Nullable
	public String getKVTable() { return null; }

	@Nullable
	public String getKVIdField() { return null; }

	public void delete(final State st) {
		final String oldname=getName();
		if (oldname.equalsIgnoreCase("Main")) {
			throw new UserInputInvalidChoiceException("You can not delete the Main menu as this is hard wired to the main HUD button");
		}
		Audit.audit(true,st,OPERATOR.AVATAR,null,null,"delete","Menus",oldname,"","Deleted menu "+oldname);
		d("delete from menus where menuid=?",getId());
	}

	protected int getNameCacheTime() { return 60*60; } // this name doesn't change, cache 1 hour

	/**
	 * Load the JSON payload for this menu.
	 *
	 * @return The JSON payload
	 */
	@Nonnull
	public JSONObject getJSON() {
		final String json=dqs("select json from menus where menuid=?",getId());
		if (json==null) { throw new SystemBadValueException("No (null) template for menu id "+getId()); }
		return new JSONObject(json);
	}

	/**
	 * Set the JSON payload.
	 *
	 * @param template JSON payload
	 */
	public void setJSON(@Nonnull final JSONObject template) {
		d("update menus set json=? where menuid=?",template.toString(),getId());
	}

	/**
	 * Obtain the instanceID this menu belongs to.
	 *
	 * @return Instance for this menu
	 */
	@Nonnull
	public Instance getInstance() {
		final int id=dqinn("select instanceid from menus where menuid=?",getId());
		return Instance.get(id);
	}

	public void flushKVCache(final State st) {}
}
