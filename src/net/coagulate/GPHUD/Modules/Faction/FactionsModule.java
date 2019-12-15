package net.coagulate.GPHUD.Modules.Faction;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Characters.CharacterAttribute;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.Modules.StaticSideSubMenu;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * Dynamic faction menu
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class FactionsModule extends ModuleAnnotation {

	public FactionsModule(String name, ModuleDefinition def) throws SystemException, UserException {
		super(name, def);
	}

	@Nonnull
	@Override
	public Set<SideSubMenu> getSideSubMenus(@Nonnull State st) {
		Set<CharacterGroup> factions = st.getInstance().getGroupsForKeyword("Faction");
		Set<SideSubMenu> ret = new HashSet<>();
		for (CharacterGroup faction : factions) {
			ret.add(new StaticSideSubMenu(faction.getName(), faction.getId(), "/factions/view/" + faction.getId(), ""));
		}
		return ret;
	}

	@Nonnull
	@Override
	public Set<CharacterAttribute> getAttributes(State st) {
		Set<CharacterAttribute> ret = new HashSet<>();
		ret.add(new FactionAttribute(-1));
		return ret;
	}


}
