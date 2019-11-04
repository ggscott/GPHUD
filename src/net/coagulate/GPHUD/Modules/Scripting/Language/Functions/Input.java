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
		vm.queueSelectCharacter(target.getContent(),message.getContent());
		vm.suspend(st,target.getContent());
		return target;
	}

	@GSFunctions.GSFunction(description = "Triggers the character's HUD to enter a string",parameters = "String - message - Description for the dialog box",notes = "",returns = "String - Some user input text")
	public static BCString gsGetText(State st, GSVM vm, BCCharacter target, BCString message) {
		vm.queueGetText(target.getContent(),message.getContent());
		vm.suspend(st,target.getContent());
		return new BCString(null,"");
	}

	@GSFunctions.GSFunction(description = "Triggers the character's HUD to offer a choice (menu box)",parameters = "String - message - Description for the dialog box",notes = "",returns = "String - The user's selection<br>List - A list of strings the user may choose from")
	public static BCString gsGetChoice(State st, GSVM vm, BCCharacter target, BCString message, BCList choices) {
		List<String> strchoices=new ArrayList<>();
		for (ByteCodeDataType s:choices.getContent()) { strchoices.add(s.toBCString().getContent()); }
		vm.queueGetChoice(target.getContent(),message.getContent(),strchoices);
		vm.suspend(st,target.getContent());
		return new BCString(null,"");
	}


}
