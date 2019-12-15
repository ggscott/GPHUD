package net.coagulate.GPHUD.Modules.User;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.DateTime;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import java.util.List;

import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;

/**
 * Views an Avatar object.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ViewAvatar {

	@URLs(url = "/avatars/view/*")
	public static void viewAvatar(@Nonnull State st, SafeMap values) throws UserException, SystemException {
		String[] split = st.getDebasedURL().split("/");
		String id = split[split.length - 1];
		User a = User.get(Integer.parseInt(id));
		viewAvatar(st, values, a);
	}


	public static void viewAvatar(@Nonnull State st, SafeMap values, @Nonnull User a) throws UserException, SystemException {
		boolean fullinstance = false;
		boolean full = false;
		String tz = st.getAvatarNullable().getTimeZone();
		if (st.getAvatarNullable() == a) {
			fullinstance = true;
			full = true;
		}
		if (st.hasPermission("Characters.ViewAll")) { fullinstance = true; }
		if (st.isSuperUser()) {
			fullinstance = true;
			full = true;
		}
		Form f = st.form();
		f.noForm();
		f.add(new TextSubHeader(a.getName()));
		if (a.getId() == User.getSystem().getId()) {
			f.p("<b>SYSTEM is a virtual avatar used as an actor for automatic events that are run by GPHUD itself, e.g. character creation, visit xp, and more</b>");
		}
		Table kvtable = new Table();
		f.add(kvtable);
		for (Char c : Char.getCharacters(st.getInstance(), a)) {
			kvtable.openRow().add("Owned Character").add(c);
		}
		String lastactive = fromUnixTime(a.getLastActive(), tz) + " " + tz;
		kvtable.openRow().add("Last Active").add(lastactive);
		kvtable.openRow().add("Selected Time Zone").add(tz);

		kvtable.openRow().add("SuperUser").add("" + a.isSuperAdmin());
		kvtable.openRow().add("DeveloperKey").add("" + a.hasDeveloperKey());
		if (!(full || fullinstance)) {
			kvtable.openRow().add("<b>Avatar</b>").add("<b>Character</b>").add("<b>Instance</b>");
			for (Char c : Char.getCharacters(a)) { kvtable.openRow().add("").add(c).add(c.getInstance()); }
			Results rows = net.coagulate.GPHUD.Data.Audit.getAudit(st.getInstance(), a, null);
			Table table = net.coagulate.GPHUD.Data.Audit.formatAudit(rows, a.getTimeZone());
			st.form().add(table);


			if (st.getAvatarNullable() != null && st.getAvatarNullable() == a) {
				kvtable.add(new Form(st, true, "../settimezone", "Set TimeZone", "timezone", tz));
			}
			//for (String key:kv.keySet()) {
			//    String value=kv.get(key);
			//    kvtable.openRow().add(key).add(value);
			//}
			if ("SYSTEM".equals(a.getName())) {
				f.add("<p><i>SYSTEM is a fake avatar used internally as an 'Invoking Avatar' for commands that usually require an active Avatar/Character, but there is no appropriate caller, e.g. Visitation XP is awarded by the SYSTEM avatar to prevent confusion and clutter in some other character/avatar's audit log</i></p>");
			}
			f.add(new TextSubHeader("Audit Trail"));
			f.add(Audit.formatAudit(Audit.getAudit(st.getInstance(), a, null), st.getAvatarNullable().getTimeZone()));
		}
	}

	@URLs(url = "/avatars/settimezone")
	public static void setTimeZone(State st, SafeMap value) {
		Modules.simpleHtml(st, "User.SetTZ", value);
	}

	@Nonnull
	@Commands(context = Command.Context.AVATAR,permitScripting = false,description = "Set displayed timezone for date/time events",permitObject = false)
	public static Response setTZ(@Nonnull State st,
	                             @Arguments(type = Argument.ArgumentType.CHOICE, description = "Prefered Time Zone", choiceMethod = "getTimeZones")
			                             String timezone) {
		st.getAvatarNullable().setTimeZone(timezone);
		return new OKResponse("TimeZone preference updated");
	}

	@Nonnull
	public static List<String> getTimeZones(State st) {
		return DateTime.getTimeZones();
	}

}
