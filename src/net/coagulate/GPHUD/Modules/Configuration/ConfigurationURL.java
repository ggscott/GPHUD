package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * fake url for side sub menus (ugly).
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConfigurationURL extends URL {

	final String name;
	final String url;
	public ConfigurationURL(String name, String url) {
		this.name = name;
		this.url = url;
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String url() {
		return url;
	}

	@Nonnull
	@Override
	public String requiresPermission() {
		return "";
	}

	@Override
	public String getFullName() {
		return name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String getMethodName() {
		throw new SystemException("Stub url has no backing method");
	}

	@Override
	public void run(State st, SafeMap values) {
		throw new SystemException("Stub url can not be run");
	}

	@Override
	public Module getModule() {
		return Modules.get(null,"Configuration");
	}

	@Override
	public boolean requiresAuthentication() {
		return true;
	}

}
