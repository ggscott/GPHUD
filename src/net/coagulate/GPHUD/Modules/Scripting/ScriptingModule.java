package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Scripts;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScriptingModule extends ModuleAnnotation {
	public ScriptingModule(final String name,
	                       final ModuleDefinition annotation) {
		super(name,annotation);
	}

	@Nonnull
	@Override
	public Command getCommandNullable(@Nonnull final State st,
	                                  @Nonnull final String commandname) {
		if (commandname.equalsIgnoreCase("characterresponse") || commandname.equalsIgnoreCase("stringresponse")) {
			return super.getCommandNullable(st,commandname);
		}
		final Scripts script=Scripts.findOrNull(st,commandname.replaceFirst("gs",""));
		if (script==null) { throw new UserInputLookupFailureException("No script named "+commandname+" exists"); }
		return new ScriptingCommand(script);
	}


	@Nonnull
	@Override
	public Map<String,Command> getCommands(@Nonnull final State st) {
		final Map<String,Command> commands=new HashMap<>();
		final Set<Scripts> scripts=st.getInstance().getScripts();
		for (final Scripts script: scripts) {
			commands.put(script.getName(),new ScriptingCommand(script));
		}
		return commands;
	}

}
