package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCLabel extends ByteCode {
	final int id;
	public BCLabel(ParseNode n,int id) {super(n);this.id=id;}
	public BCLabel(ParseNode n,int id,int address) {super(n);this.id=id; this.address=address; }
	@Nonnull
	public String explain() { return "Label (:"+id+")"; }
	@Nullable
	Integer address=null;
	public int address() {
		if (address==null) { throw new SystemException("Jump address is null"); }
		return address;
	}
	public void toByteCode(@Nonnull List<Byte> bytes) {
		address=bytes.size();
	}

	@Override
	public void execute(State st, GSVM vm, boolean simulation) {
		throw new GSInternalError("Can not execute the LABEL instruction, it is a pseudocode marker for compilation only");
	}
}
