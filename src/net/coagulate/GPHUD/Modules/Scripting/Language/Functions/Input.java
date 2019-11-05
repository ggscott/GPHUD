package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCList;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import java.util.ArrayList;
import java.util.List;

public class Input {

	@GSFunctions.GSFunction(description = "Triggers the character's HUD to select a nearby character",parameters = "String - message - Description for the dialog box",notes = "",returns = "Character - a character the user selected")
	public static BCCharacter gsSelectCharacter(State st, GSVM vm, BCCharacter target, BCString message) {
		if (vm.simulation) { vm.suspend(st,st.getCharacter()); return target; }
		vm.queueSelectCharacter(target.getContent(),message.getContent());
		vm.suspend(st,target.getContent());
		return target;
	}

	@GSFunctions.GSFunction(description = "Triggers the character's HUD to enter a string",parameters = "String - message - Description for the dialog box",notes = "",returns = "String - Some user input text")
	public static BCString gsGetText(State st, GSVM vm, BCCharacter target, BCString message) {
		if (vm.simulation) { vm.suspend(st,st.getCharacter()); return new BCString(null,"Simulated user input"); }
		vm.queueGetText(target.getContent(),message.getContent());
		vm.suspend(st,target.getContent());
		return new BCString(null,"");
	}

	@GSFunctions.GSFunction(description = "Triggers the character's HUD to offer a choice (menu box)",parameters = "String - message - Description for the dialog box",notes = "",returns = "String - The user's selection<br>List - A list of strings the user may choose from")
	public static BCString gsGetChoice(State st, GSVM vm, BCCharacter target, BCString message, BCList choices) {
		if (vm.simulation) {
			vm.suspend(st,st.getCharacter());
			if (choices.getContent().isEmpty()) { return new BCString(null,"Simulation with empty choice list"); }
			else { return choices.getContent().get(0).toBCString(); }
		}
		List<String> strchoices=new ArrayList<>();
		for (ByteCodeDataType s:choices.getContent()) { strchoices.add(s.toBCString().getContent()); }
		vm.queueGetChoice(target.getContent(),message.getContent(),strchoices);
		vm.suspend(st,target.getContent());
		return new BCString(null,"");
	}


}
