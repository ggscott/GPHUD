package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class GenericXPPool extends Pool {
	private final String myname;

	public GenericXPPool(String name) {
		super();
		myname = name;
	}

	@Override
	public boolean isGenerated() { return true; }

	@Nonnull
	@Override
	public String description() { return myname + " pool"; }

	@Nonnull
	@Override
	public String fullName() { return "Experience." + myname; }

	@Nonnull
	@Override
	public String name() { return myname; }

	public void awardXP(@Nonnull State st, @Nonnull Char target, @Nullable String reason, int ammount, boolean incontext) {
		State targetstate = State.getNonSpatial(target);
		float period = targetstate.getKV(fullName() + "XPPeriod").floatValue();
		int maxxp = targetstate.getKV(fullName() + "XPLimit").intValue();
		Pool pool = Modules.getPool(targetstate, "Experience." + myname + "XP");
		int awarded = target.sumPoolDays(pool, period);
		if (awarded >= maxxp) {
			throw new UserException("This character has already reached their " + pool.name() + " XP limit.  They will next be eligable for a point in " + target.poolNextFree(pool, maxxp, period));
		}
		if ((awarded + ammount) > maxxp) {
			throw new UserException("This will push the character beyond their " + pool.name() + " XP limit, they can be awarded " + (maxxp - awarded) + " XP right now");
		}
		if (reason==null) { throw new UserException("You must supply a reason"); }
		// else award xp :P
		Audit.audit(st, Audit.OPERATOR.CHARACTER, null, target, "Pool Add", pool.name() + "XP", null, ammount + "", reason);
		target.addPool(st, pool, ammount, reason);
		if (target != st.getCharacter()) {
			if (incontext) {
				target.hudMessage("You were granted " + ammount + " point" + (ammount == 1 ? "" : "s") + " of " + pool.name() + " XP by " + st.getCharacter().getName() + " for " + reason);
			} else {
				target.hudMessage("You were granted " + ammount + " point" + (ammount == 1 ? "" : "s") + " of " + pool.name() + " XP by ((" + st.getAvatar().getName() + ")) for " + reason);
			}
		}
	}

}
