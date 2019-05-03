package net.coagulate.GPHUD.Modules;

import java.lang.annotation.*;

/**
 * Wraps a side sub menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class SideSubMenu {

	public abstract String name();

	public abstract int priority();

	public abstract String requiresPermission();

	public abstract boolean isGenerated();

	public abstract String getURL();

	/**
	 * Defines a sidemenu "sub" link.
	 * Must be connected to a method that also contains an @URLs
	 *
	 * @see @URL
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	public @interface SideSubMenus {
		String name();

		int priority();

		String requiresPermission() default "";
	}

}
