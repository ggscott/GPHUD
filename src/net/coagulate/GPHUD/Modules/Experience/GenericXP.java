package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Implements Visit XP awards.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class GenericXP extends QuotaedXP {
	private final String myname;

	public GenericXP(String name) {
		super(-1);
		myname = name;
	}

	public String getName() { return myname; }

	@Nonnull
	public String poolName(State st) {return "Experience." + myname;}

	@Nonnull
	public String quotaKV(State st) {return "Experience." + myname + "Limit"; }

	@Nonnull
	public String periodKV(State st) { return "Experience." + myname + "Period"; }

	public Module getModule() { return Modules.get(null, "Experience"); }

}
