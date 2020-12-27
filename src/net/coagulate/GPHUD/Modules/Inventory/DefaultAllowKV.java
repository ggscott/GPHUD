package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class DefaultAllowKV extends KV {

	@Nonnull final Attribute inventory;

	public DefaultAllowKV(@Nonnull final Attribute inventory) {
		this.inventory=inventory;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String fullname() {
		return "Inventory."+name();
	}

	@Nonnull
	@Override
	public KVSCOPE scope() {
		return KVSCOPE.INSTANCE;
	}

	@Nonnull
	@Override
	public KVTYPE type() {
		return KVTYPE.BOOLEAN;
	}

	@Nonnull
	@Override
	public String description() {
		return "Wether an inventory "+inventory.getName()+" is allows to store new items by default";
	}

	@Nonnull
	@Override
	public String editpermission() {
		return "Inventory.ConfigureAccess";
	}

	@Nonnull
	@Override
	public String defaultvalue() {
		return "true";
	}

	@Nonnull
	@Override
	public String conveyas() {
		return "";
	}

	@Nonnull
	@Override
	public KVHIERARCHY hierarchy() {
		return KVHIERARCHY.DELEGATING;
	}

	@Override
	public boolean template() { return false; }

	@Nonnull
	@Override
	public String name() {
		return inventory.getName()+"DefaultAllow";
	}

}
