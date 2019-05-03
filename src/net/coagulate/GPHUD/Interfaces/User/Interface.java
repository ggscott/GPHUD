package net.coagulate.GPHUD.Interfaces.User;

import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.PrimaryCharacters;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.PasswordInput;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.Text;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.logging.Level.*;


/**
 * Handles User HTML connections.
 * <p>
 * I.e. the start of the HTML interface.
 *
 * @author iain
 */
public class Interface extends net.coagulate.GPHUD.Interface {

	// leave this here for now
	public static String styleSheet() {
		return "" +
				"<style>\n" +
				".tooltip {\n" +
				"    position: relative;\n" +
				"    display: inline-block;\n" +
				"    border-bottom: 1px dotted black; /* If you want dots under the hoverable text */\n" +
				"}\n" +
				"\n" +
				".tooltip .tooltiptext {\n" +
				"    visibility: hidden;\n" +
//"    width: 120px;\n" +
				"    background-color: #e0e0e0;\n" +
				"    color: black;\n" +
				"    text-align: center;\n" +
				"    padding: 5px 0;\n" +
				"    border-radius: 6px;\n" +
				" \n" +
				"     top: -5px;\n" +
				"    left: 105%; " +
				"    position: absolute;\n" +
				"    z-index: 1;\n" +
				"white-space: nowrap;\n" +
				"}\n" +
				"\n" +
				"/* Show the tooltip text when you mouse over the tooltip container */\n" +
				".tooltip:hover .tooltiptext {\n" +
				"    visibility: visible;\n" +
				"}\n" +
				"</style>";
	}

	/**
	 * URLs we do not redirect to instance/char selection.  Like logout.
	 */
	private boolean interceptable(String url) {
		//System.out.println(url);
		if ("/logout".equalsIgnoreCase(url)) { return false; }
		if ("/Help".equalsIgnoreCase(url)) { return false; }
		return true;
	}

	/**
	 * this is where the request comes in after generic processing.
	 * We basically just encapsulate all requests in an Exception handler that will spew errors as HTML errors (rather than JSON errors).
	 * These are rather useless in production, but in DEV we dump the stack traces too.
	 *
	 * @param st
	 */
	@Override
	public void process(State st) {
		st.source = State.Sources.USER;
		//for (Header h:headers) { System.out.println(h.getName()+"="+h.getValue()); }

		try {
			st.resp.setStatusCode(HttpStatus.SC_OK);
			st.resp.setEntity(new StringEntity(renderHTML(st), ContentType.TEXT_HTML));
		} catch (RedirectionException redir) {
			st.resp.setStatusCode(303);
			String targeturl = redir.getURL();
			//System.out.println("PRE:"+targeturl);
			if (targeturl.startsWith("/") && !targeturl.startsWith("/GPHUD")) { targeturl = "/GPHUD" + targeturl; }
			//System.out.println("POST:"+targeturl);
			st.resp.addHeader("Location", targeturl);
		} catch (Exception e) {
			try {
				SL.report("GPHUD UserInterface exception", e, st);
				GPHUD.getLogger().log(SEVERE, "UserInterface exception : " + e.getLocalizedMessage(), e);
				// stash the exception for the ErrorPage
				st.exception = e;
				st.resp.setStatusCode(HttpStatus.SC_OK);
				// an unmapped page, "ErrorPage"
				st.resp.setEntity(new StringEntity("Error.", ContentType.TEXT_HTML));
			} catch (Exception ex) {
				SL.report("GPHUD UserInterface exception in exception handler", ex, st);
				GPHUD.getLogger().log(SEVERE, "Exception in exception handler - " + ex.getLocalizedMessage(), ex);
			}
		}
	}

	public String messages(State st) {
		if (st.getCharacterNullable() == null) { return ""; }
		int messages = st.getCharacter().messages();
		if (messages > 0) {
			return "<p>" + new Link("<b>You have " + messages + " unread message" + (messages > 1 ? "s" : "") + ", click here to read</b>", "/GPHUD/messages/list").asHtml(st, true) + "</p>";
		}
		return "";
	}

	public String renderHTML(State st) {
		// This is basically the page template, the supporting structure that surrounds a title/menu/body
		String body = renderBodyProtected(st);
		String p = "";
		p += "<html><head><title>";
		p += "GPHUD";
		p += "</title>";
		p += styleSheet();
		p += "<link rel=\"shortcut icon\" href=\"/resources/icon-gphud.png\">";
		p += "</head><body>";
		p += "<table height=100% valign=top><tr><td colspan=3 align=center width=100%>";
		p += "<table style=\"margin: 0px; border:0px;\" width=100%><tr><td width=33% align=left>";
		p += "<h1 style=\"margin: 0px;\"><img src=\"/resources/banner-gphud.png\"></h1>";
		p += "</td><td width=34% align=center>";
		String middletarget = "/resources/banner-gphud.png";
		if (st.getInstanceNullable() != null) {
			middletarget = st.getInstance().getLogoURL(st);
		}
		p += "<h1 style=\"margin: 0px;\"><img src=\"" + middletarget + "\" height=100px></h1>";
		p += "</td><td width=33% align=right>";
		p += "<h1 style=\"margin: 0px;\"><a href=\"/\">" + SL.getBannerHREF() + "</a></h1>";
		p += "</td></tr></table>";
		//p+="<i>"+GPHUD.environment()+"</i>";
		p += "<hr>";
		p += "</td></tr>";
		p += "<tr height=100% valign=top>";
		p += "<td width=150px valign=top height=100%>";
		// calculate body first, since this sets up auth etc which the side menu will want to use to figure things out.

		try {
			p += renderSideMenu(st);
		} catch (Exception e) {
			// Exception in side menu code
			p += "<b><i>Crashed</i></b>";
			SL.report("GPHUD Side Menu crashed", e, st);
			st.logger().log(WARNING, "Side menu implementation crashed", e);
		}
		p += "</td><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td width=100% valign=top height=100%>";
		p += messages(st);
		p += body;
		p += "</td>";
		p += "</tr>";
		p += "</table></body></html>";
		return p;
	}

	protected String renderBodyProtected(State st) {
		// with all exception protections, in a 'sensible' way.
		try {
			return renderBody(st);
		} catch (Exception t) {
			// Exception in processing the command
			if (t instanceof RedirectionException) { throw (RedirectionException) t; }
			try {
				t.printStackTrace();
				if (t instanceof UserException) {
					String r = "<h1>ERROR</h1><p>Sorry, your request could not be completed<br><pre>" + t.getLocalizedMessage() + "</pre></p>";
					GPHUD.getLogger().log(INFO, "Page threw user mode exception " + t.toString());
					if (GPHUD.DEV) {
						r += "<hr><h1 align=center>DEV MODE</h1><hr><h1>User Mode Exception</h1>" + ExceptionTools.dumpException(t) + "<Br><br>" + st.toHTML();
						SL.report("GPHUD Web User Exception", t, st);
						GPHUD.getLogger().log(WARNING, "Page threw user mode exception", t);
					}
					return r;
				}
				SL.report("GPHUD Web Other Exception", t, st);
				GPHUD.getLogger().log(WARNING, "Page threw non user exception", t);
				String r = "<h1>INTERNAL ERROR</h1><p>Sorry, your request could not be completed due to an internal error.</p>";
				if (GPHUD.DEV) {
					r += "<hr><h1 align=center>DEV MODE</h1><hr><h1>System Mode Exception</h1>" + ExceptionTools.dumpException(t) + "<Br><br>" + st.toHTML();
				}
				return r;
			} catch (Exception f) {
				GPHUD.getLogger().log(SEVERE, "Exception in exception handler", f);
				return "EXCEPTION IN EXCEPTION HANDLER, PANIC!";
			}
		}
	}

	String dynamicSubMenus(State st, SideMenu menu) {
		// dereference the menu into a module
		Module owner = null;
		for (Module m : Modules.getModules()) { if (m.getSideMenu(st) == menu) { owner = m; } }
		if (owner == null) { return ">> NULL?<br>"; }

		String ret = "";
		Map<Integer, Set<SideSubMenu>> priorities = new TreeMap<>();
		// collect sidemenus, by priority
		for (SideSubMenu s : owner.getSideSubMenus(st)) {
			Integer priority = s.priority();
			Set<SideSubMenu> set = new HashSet<>();
			if (priorities.containsKey(priority)) { set = priorities.get(priority); }
			set.add(s);
			priorities.put(s.priority(), set);
		}
		// enumerate the priorities
		for (Integer pri : priorities.keySet()) {
			// enumerate the SideMenus
			for (SideSubMenu s : priorities.get(pri)) {
				String u = s.getURL();
				ret += "&nbsp;&nbsp;&nbsp;&gt;&nbsp;&nbsp;&nbsp;<a href=\"/GPHUD" + u + "\">" + s.name() + "</a><br>";
			}
		}
		return ret;
	}

	String dynamicSideMenus(State st) throws UserException, SystemException {
		String r = "";
		Map<Integer, Set<SideMenu>> priorities = new TreeMap<>();
		// collect sidemenus, by priority
		for (Module m : Modules.getModules()) {
			if (m.isEnabled(st)) {
				SideMenu s = m.getSideMenu(st);
				if (s != null) {
					boolean permitted = true;
					if (!s.requiresPermission().isEmpty()) {
						if (!st.hasPermission(s.requiresPermission())) {
							permitted = false;
						}
					}
					if (permitted) {
						Integer priority = s.priority();
						Set<SideMenu> set = new HashSet<>();
						if (priorities.containsKey(priority)) { set = priorities.get(priority); }
						set.add(s);
						priorities.put(s.priority(), set);
					}
				}
			}
		}
		// enumerate the priorities
		for (Integer pri : priorities.keySet()) {
			// enumerate the SideMenus
			for (SideMenu menu : priorities.get(pri)) {
				String url = menu.url();
				String name = menu.name();
				r += "<a href=\"/GPHUD" + url + "\">" + name + "</a><br>";
				if (st.getDebasedURL().startsWith(url)) {
					r += dynamicSubMenus(st, menu);
				}
			}
		}


		return r;
	}

	String renderSideMenu(State st) throws UserException, SystemException {
		String s = "";
		s += GPHUD.menuPanelEnvironment() + "<hr width=150px>";
		boolean loggedin = true;
		if (st.getCharacterNullable() != null || st.getAvatar() != null) {
			s += "<b>Avatar:</b> ";
			//if (st.user!=null) s+="[<a href=\"/GPHUD/switch/avatar\">Switch</a>]"; // you can only switch avis if you're a logged in user, as thats what binds avis
			s += "<br>";
			if (st.avatar() != null) {
				s += st.avatar().getGPHUDLink() + "<br>";
			} else { s += "<i>none</i><br>"; }


			s += "<b>Instance:</b> [<a href=\"/GPHUD/switch/instance\">Switch</a>]<br>";
			if (st.getInstanceNullable() != null) {
				s += st.getInstance().asHtml(st, true) + "<br>";
			} else { s += "<i>none</i><br>"; }

			s += "<b>Character:</b> [<a href=\"/GPHUD/switch/character\">Switch</a>]<br>";
			if (st.getCharacterNullable() != null) {
				s += st.getCharacter().asHtml(st, true) + "<br>";
			} else { s += "<i>none</i><br>"; }
		} else {
			s += "<i>Not logged in</i><hr width=150px><a href=\"/GPHUD/\">Index</a><br><br>";
			s += "<a href=\"/GPHUD/Help\">Documentation</a><br>";
			s += "<hr width=150px>";
			return s;
		}
		if (loggedin) {
			s += "<br><a href=\"/GPHUD/logout\">Logout</a><br>";
		}
		s += "<hr width=150px>";
		s += "<a href=\"/GPHUD/\">Index</a><br><br>";
		boolean dynamics = true;
		if (st.avatar() == null) {
			s += "<i>Select an avatar</i><br>";
			dynamics = false;
		}
		if (st.getInstanceNullable() == null) {
			s += "<i>Select an instance</i><br>";
			dynamics = false;
		}
		if (dynamics) {
			s += dynamicSideMenus(st);
			s += "<br>";
		}
		s += "<a href=\"/GPHUD/Help\">Documentation</a><br>";
		s += "<hr width=150px>";
		String sectionhead = "<b>PERMISSIONS:</b><br>";
		if (st.isSuperUser()) {
			s += sectionhead + "<b style=\"color: blue;\">SUPER-ADMIN</b><br>";
			sectionhead = "";
		}
		if (st.isInstanceOwner()) {
			s += sectionhead + "<b style=\"color: blue;\">Instance Owner</b><br>";
			sectionhead = "";
		}
		if (st.getPermissions() != null) {
			for (String permission : st.getPermissions()) {
				s += sectionhead + "<font style=\"color: green;\">" + permission + "</font><br>";
				sectionhead = "";
			}
		}
		return s;
	}

	public boolean isRich() { return true; }

	public String renderBody(State st) throws SystemException, UserException {
		Form f = null;
		SafeMap values = getPostValues(st);
		URL content = Modules.getURL(st, st.getDebasedNoQueryURL());
		if (content.requiresAuthentication()) {
			f = authenticationHook(st, values);
			if (st.getInstanceNullable() == null && st.getCharacterNullable() != null) {
				st.setInstance(st.getCharacter().getInstance());
			}
			// redirect the request if still no data
			if (st.getInstanceNullable() == null) {
				//hmm.  PrimaryChar doesn't work because thats instance dependant.
				// just remap the URL.  and for that matter we need an exclusion list
				if (interceptable(st.getDebasedNoQueryURL())) { st.setURL("/switch/instance"); }
			}
		}
		if (f == null) {
			f = new Form();
			st.form = f;
			if (!content.requiresPermission().isEmpty()) {
				if (!st.hasPermission(content.requiresPermission())) {
					st.logger().log(WARNING, "Attempted access to " + st.getDebasedURL() + " which requires missing permission " + content.requiresPermission());
					throw new UserException("Access to this page is denied, you require permission " + content.requiresPermission());
				}
			}
			content.run(st, values);
			for (String value : values.keySet()) { f.readValue(value, values.get(value)); }
		} else { st.form = f; }

		return f.asHtml(st, isRich());
	}

	public SafeMap getPostValues(State st) {
		SafeMap values = new SafeMap();
		Form f = st.form;
		HttpRequest req = st.req;
		// needs to have an entity to be a post
		if (req instanceof HttpEntityEnclosingRequest) {
			InputStream contentstream = null;
			try {
				// cast it
				HttpEntityEnclosingRequest entityrequest = (HttpEntityEnclosingRequest) req;
				HttpEntity entity = entityrequest.getEntity();
				// the content, as a stream (:/)
				contentstream = entity.getContent();

				// make a buffer, read, make a string, voila :P
				int available = contentstream.available();
				if (available == 0) { return values; } //not actually a post
				byte array[] = new byte[available];
				contentstream.read(array);
				String content = new String(array);
				// parse the string into post variables
				//System.out.println(content);
				// this should probably be done "properly"
				String[] parts = content.split("&");
				for (String part : parts) {
					String[] keyvalue = part.split("=");
					String key = URLDecoder.decode(keyvalue[0], "UTF-8");
					String value = "";
					if (keyvalue.length > 1) { value = URLDecoder.decode(keyvalue[1], "UTF-8"); }
					if (keyvalue.length > 2) {
						throw new SystemException("Unexpected parsing of line '" + part + "' - got " + keyvalue.length + " fields");
					}
					if (value != null && !value.isEmpty()) { values.put(key, value); }
					//System.out.println("HTTP POST ["+key+"]=["+value+"]");
				}
			} catch (IOException ex) {
				st.logger().log(SEVERE, "Unexpected IOException reading form post data?", ex);
			} catch (UnsupportedOperationException ex) {
				st.logger().log(WARNING, "Unsupported Operation Exception reading form post data?", ex);
			} finally {
				try {
					if (contentstream != null) { contentstream.close(); }
				} catch (IOException ex) {
					st.logger().log(WARNING, "Unexpected IOException closing stream after primary exception?", ex);
				}
			}
		}
		return values;
	}

	protected boolean cookieAuthenticationOnly() { return false; }

	// override me if you want to disable authentication or something :P
	// return "null" to proceed with normal stuff (modify the context, store auth results here).
	// return a Form if you want to intercept the connection to authenticate it
	public Form authenticationHook(State st, SafeMap values) throws SystemException {
		boolean debug = false;
		if (debug) { System.out.println("Calling authentication hook, URI is " + st.getDebasedURL()); }
		String cookie = null;
		String coagulateslcookie = null;
		for (Header h : st.req.getHeaders("Cookie")) {
			for (String piece : h.getValue().split(";")) {
				if (debug) { System.out.println("Have a cookie: " + piece); }
				piece = piece.trim();
				if (piece.startsWith("coagulateslsessionid=")) {
					coagulateslcookie = piece.substring("coagulateslsessionid=".length());
					if (debug) { System.out.println("Extracted coagulate SL cookie from header " + coagulateslcookie); }
				}
				if (piece.startsWith("gphud=")) {
					cookie = piece.substring(6);
					if (debug) { System.out.println("Extracted cookie from header " + cookie); }
				}
			}
		}
		String array[] = st.getDebasedURL().split("\\?"); // URLs passed always takes precedence
		for (String piece : array) {
			if (piece.startsWith("gphud=")) {
				cookie = piece.substring("gphud=".length());
				if (debug) { System.out.println("Extracted cookie from URI " + cookie); }
				st.resp.addHeader("Set-Cookie", "gphud=" + cookie + "; Path=/");
				st.setURL(st.getFullURL().replaceAll("\\?gphud=.*", ""));
				if (!(this instanceof net.coagulate.GPHUD.Interfaces.HUD.Interface)) {
					if (debug) { System.out.println("Redirecting to " + st.getDebasedURL()); }
					throw new RedirectionException(st.getDebasedURL());
				}
			}
		}
		Cookies cookies = null;
		if (cookie != null) {
			if (debug) { System.out.println("getting cookie"); }
			try {
				cookies = new Cookies(cookie);
			} catch (SystemException e) {} // logged out possibly, or expired and cleaned up
		}
		if (debug) { System.out.println("Cookies object is " + cookies); }
		if (cookies != null) {
			Instance instance = cookies.getInstance();
			if (instance != null) { st.setInstance(instance); }
			User av = cookies.getAvatar();
			Char ch = cookies.getCharacter();
			if (av != null) {
				st.setAvatar(av);
			}
			if (ch != null) {
				st.setCharacter(ch);
			}
			if (av == null && ch != null) { st.setAvatar(ch.getOwner()); }
			if (av != null) {
				st.cookiestring = cookie;
				st.cookie = cookies;
				return characterSelectionHook(st, values);
			} // logged in, one way or the other, note we might not have an entity, and we want one
		}
		if (cookies == null && coagulateslcookie != null && !coagulateslcookie.isEmpty()) {
			Session slsession = Session.get(coagulateslcookie);
			if (slsession != null) {
				if (debug) { System.out.println("Adopting Coagulate SL Session (?)"); }
				User av = slsession.user();
				if (av != null) {
					st.setAvatar(av);
					Char defaultchar = null;
					try {
						defaultchar = PrimaryCharacters.getPrimaryCharacter(st, false);
					} catch (UserException e) {} // may have no characters etc
					Instance instance = null;
					if (defaultchar != null) {
						instance = defaultchar.getInstance();
						st.setInstance(instance);
						st.setCharacter(defaultchar);
					}
					cookie = Cookies.generate(av, defaultchar, instance, true);
					st.cookiestring = cookie;
					try {
						st.cookie = new Cookies(cookie);
					} catch (SystemException ex) {
						st.logger().log(SEVERE, "Cookie load gave exception, right after it was generated?", ex);
					}
					st.resp.addHeader("Set-Cookie", "gphud=" + cookie + "; Path=/");
					st.logger().log(INFO, "SL Cluster Services SSO as " + av);
					return characterSelectionHook(st, values);
				}
			}
		}
		if (cookieAuthenticationOnly()) {
			Form failed = new Form();
			if (cookie != null && !"".equals(cookie)) {
				failed.add("Sorry, your session has expired, please start a new session somehow");
			} else {
				failed.add("Sorry, login failed, cookie not received at this time.");
			}
			return failed;
		}
		// assume we authenticated!
		// this assumption eventually breaks as we put no login details in the context for later use
		Form login = new Form();
		Text topline = new Text("");
		login.add(topline);
		login.add("<h3>Welcome to GPHUD</h3><p>Please provide authentication:</p>");
		Table t = new Table();
		t.add("Username:").add(new TextInput("username")).closeRow();
		t.add("Password:").add(new PasswordInput("password")).closeRow();
		t.add(new Button("Submit"));
		login.add(t);
		st.form = login;
		String username = values.get("username");
		String password = values.get("password");
		String failed = "";
		if ("Submit".equals(values.get("Submit")) && !(username.isEmpty()) && !(password.isEmpty())) {
			User target = User.findOptional(username);
			if (target == null) {
				failed = "Incorrect credentials.";
				st.logger().log(WARNING, "Attempt to login as '" + username + "' failed, no such user.");
			} else {
				if (target.checkPassword(password)) {
					cookie = Cookies.generate(target, null, null, true);
					st.username = username;
					st.avatar = target;
					st.cookiestring = cookie;
					try {
						st.cookie = new Cookies(cookie);
					} catch (SystemException ex) {
						st.logger().log(SEVERE, "Cookie load gave exception, right after it was generated?", ex);
					}
					st.resp.addHeader("Set-Cookie", "gphud=" + cookie + "; Path=/");
					st.logger().log(INFO, "Logged in from " + st.address.getHostAddress());
					return characterSelectionHook(st, values);
				} else {
					st.logger().log(WARNING, "Attempt to login as '" + username + "' failed, wrong password.");
					failed = "Incorrect credentials.";
				}
			}
		}
		login.add(failed);
		return login;
	}

	// A login must select an avatar from its list of avatars, if it has more than one...
	private Form characterSelectionHook(State st, Map<String, String> values) {
		if (1 == 1) { return null; }
		if (st.getCharacter() != null) { return null; } // already have one from cookie etc
		Set<Char> characters = Char.getCharacters(st.getInstance(), st.getAvatar());
		//if (characters.isEmpty()) { Form f=new Form(); f.add("You have no active characters at any instances, please visit an instance to commence."); return f; }
		// technically you should be able to do stuff as an avatar alone, but...
		if (characters.isEmpty()) { return null; }
		if (characters.size() == 1) {
			st.setCharacter(characters.iterator().next());
			st.cookie.setCharacter(st.getCharacter());
			return null;
		}
		Form selectavatars = new Form();
		selectavatars.add(new TextHeader("Select a character"));
		Map<Button, Char> buttons = new TreeMap<>();
		for (Char e : characters) {
			Button b = new Button(e.getName());
			buttons.put(b, e);
			selectavatars.add(b);
			selectavatars.add("<br>");
		}
		st.form = selectavatars;
		for (Char e : characters) {
			if (values.get(e.getName()) != null && !values.get(e.getName()).isEmpty()) {
				st.setCharacter(e);
				st.cookie.setCharacter(st.getCharacter());
				return null;
			}
		}
		return selectavatars;
	}

}
