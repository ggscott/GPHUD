@ModuleDefinition(canDisable = false, description = "Support for Aliasing existing commands to create instance specific usages", implementation = "net.coagulate.GPHUD.Modules.Alias.AliasModule", forceConfig = true)
@Permissions(description = "Permission to create modify and delete aliases", name = "Config")
package net.coagulate.GPHUD.Modules.Alias;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.Permissions;

/**
 * The Alias module allows the instance to create customised versions of commands.
 * <p>
 * An alias is a JSON Object template which supplies some default values for various parameters, those parameters that are not templated may be decided by the eventual user.
 * The templated command is exposed as a new generated command in the Alias namespace.
 *
 * @author Iain Price <gphud@predestined.net>
 */
