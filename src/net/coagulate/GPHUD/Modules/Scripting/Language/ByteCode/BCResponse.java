package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Interfaces.Responses.Response;

import java.util.List;

public class BCResponse extends ByteCode {
	private Response content=null;
	public BCResponse() {}
	public BCResponse(Response content) { this.content=content; }
	public String explain() { return "Response("+content+") (push)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Response.get());
		//throw new SystemException("Not implemented");
	}
}
