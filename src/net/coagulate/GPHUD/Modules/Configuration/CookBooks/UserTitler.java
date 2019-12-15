package net.coagulate.GPHUD.Modules.Configuration.CookBooks;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class UserTitler extends CookBook {
	@URL.URLs(url = "/configuration/cookbooks/user-titler")
	public static void createForm(@Nonnull State st, @Nonnull SafeMap values) throws UserException, SystemException {
		Form f = st.form();
		f.add(new TextHeader("User Configurable Titler Cookbook"));
		boolean act = false;
		f.add(new Paragraph("This cookbook will enable the user to append their own textual data to the Titler text, it will perform the following steps:"));
		Table t = new Table();
		f.add(t);
		run(st, t, false);
		if (values.containsKey("ACTIVATE COOKBOOK") && (st.hasPermission("instance.cookbooks"))) {
			f.add("");
			f.add(new TextSubHeader("EXECUTING COOKBOOK"));
			Table runt = new Table();
			f.add(runt);
			run(st, runt, true);
		} else {
			confirmButton(st, f);
		}
	}

	private static void run(@Nonnull State st, @Nonnull Table t, boolean act) {
		t.add(new HeaderRow().add("Action").add("Verification").add("Description"));
		charAttribute(st, act, t, "TitlerText", "true", "TEXT", "", "FALSE", "FALSE", "");
		String newvalue;//=st.getRawKV(st.getInstance(), "GPHUDClient.TitlerText");
		newvalue = "--NAME----NEWLINE----TITLERTEXT--";
		setKV(st, act, t, st.getInstance(), "GPHUDClient.TitlerText", newvalue);
		JSONObject mappings = new JSONObject();
		mappings.put("attribute", "TitlerText");
		mappings.put("value-desc", "Enter new titler text");
		createAlias(st, act, t, "SetTitlerText", "characters.set", mappings);
		menu(st, act, t, "Titler Text", "Alias.SetTitlerText");
	}

}
