package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GSCompiler {

	private int jumpnumber=1;
	private int expectedChildren(final ParseNode node) {
		if (node instanceof GSStart) { return -1; }
		if (node instanceof GSInitialiser) { return 3; }
		if (node instanceof GSExpression) { return 1; }
		if (node instanceof GSParameter) { return 1; }
		if (node instanceof GSTerm) { return 1; }
		if (node instanceof GSFunctionCall) { return -1; }
		if (node instanceof GSStringConstant) { return 0; }
		if (node instanceof GSIntegerConstant) { return 0; }
		if (node instanceof GSIdentifier) { return 0; }
		if (node instanceof GSParameters) { return -1; }
		if (node instanceof GSConditional) { return -1; }
		if (node instanceof GSAssignment) { return 2; }
		if (node instanceof GSStatement) { return -1; }
		if (node instanceof GSBinaryOperation) { return 3; }
		if (node instanceof GSList) { return -1; }
		if (node instanceof GSListIndex) { return 2; }
		if (node instanceof GSWhileLoop) { return 2; }
		throw new SystemImplementationException("Expected Children not defined for node "+node.getClass().getName()+" at line "+node.jjtGetFirstToken().beginLine+", column "+node.jjtGetFirstToken().beginColumn);
	}
	@Nonnull
	private final ParseNode startnode;
	public GSCompiler(final Node passednode) {
		if (!(passednode instanceof ParseNode)) { throw new SystemImplementationException("Compiler error - passed node is of type "+passednode.getClass().getCanonicalName()+" which is not a ParseNode implementation"); }
		startnode=(ParseNode) passednode;
	}

	@Nonnull
	public Byte[] toByteCode() {
		final List<ByteCode> bytecodelist=compile();
		// twopass
		List<Byte> bytecode=new ArrayList<>();
		for (final ByteCode bc:bytecodelist) { bc.toByteCode(bytecode); }
		// redo. now that forward references are completed
		bytecode=new ArrayList<>();
		for (final ByteCode bc:bytecodelist) { bc.toByteCode(bytecode); }
		return bytecode.toArray(new Byte[]{});
	}
	// The compiler has a stack (Last In First Out) which it uses to store 'things'
	// we also have a 'script' which is just a list of things, this time including instructions
	@Nonnull
	public List<ByteCode> compile() { lastdebuglineno=-1; lastdebugcolno=-1; return compile(startnode); }

	private int lastdebuglineno=-1;
	private int lastdebugcolno=-1;
	private void addDebug(@Nonnull final List<ByteCode> compiled, @Nonnull final ParseNode node) {
		final Token firsttoken=node.jjtGetFirstToken();
		if (firsttoken!=null) {
			final int lineno = firsttoken.beginLine;
			final int colno = firsttoken.beginColumn;
			if (lineno != lastdebuglineno || colno != lastdebugcolno) {
				compiled.add(new BCDebug(node,lineno, colno));
				lastdebuglineno = lineno;
				lastdebugcolno = colno;
			}
		}
	}
	@Nonnull
	private List<ByteCode> compile(@Nonnull final ParseNode node) {
		final List<ByteCode> compiled=new ArrayList<>();
		if (expectedChildren(node)>-1 && node.jjtGetNumChildren()!=expectedChildren(node)) { throw new SystemImplementationException(node.getClass().getSimpleName()+" had "+node.jjtGetNumChildren()+" children, expected "+expectedChildren(node)+" at line "+node.jjtGetFirstToken().beginLine+", column "+node.jjtGetFirstToken().beginColumn); }


		if (node instanceof GSStart || node instanceof GSExpression || node instanceof GSParameter || node instanceof GSTerm || node instanceof GSStatement) { // expression just breaks down into 1 of X executable subtypes
			// Start expands to a list of Statement (types)
			for (int i=0;i<node.jjtGetNumChildren();i++) {
				compiled.addAll(compile(node.child(i)));
			}
			return compiled;
		}

		if (node instanceof GSInitialiser) {
			// 3 children node, type, name, value
			checkType(node,0, GSTypeSpecifier.class);
			checkType(node,1, GSIdentifier.class);
			checkType(node,2, GSExpression.class);
			boolean typed=false;
			final String type=node.child(0).tokens();
			// INITIALISE the variable - reverse place name and null content.  Then we just implement "set variable".
			if (type.equals("String")) { compiled.add(new BCString(node)); typed=true; }
			if (type.equals("Response")) { compiled.add(new BCResponse(node)); typed=true; }
			if (type.equals("Integer")) { compiled.add(new BCInteger(node)); typed=true; }
			if (type.equals("Avatar")) { compiled.add(new BCAvatar(node)); typed=true; }
			if (type.equals("Group")) { compiled.add(new BCGroup(node)); typed=true; }
			if (type.equals("Character")) { compiled.add(new BCCharacter(node)); typed=true; }
			if (type.equals("List")) { compiled.add(new BCList(node)); typed=true; }
			if (!typed) { throw new SystemImplementationException("Unable to initialise variable of type "+type+" (not implemented)"); }
			final BCString identifier=new BCString(node.child(1),node.child(1).tokens());
			compiled.add(identifier);
			addDebug(compiled,node);
			compiled.add(new BCInitialise(node));
			// variable initialised.  Assign variable wants to pop the name then content, so content first
			compiled.addAll(compile(node.child(2)));
			compiled.add(identifier);
			addDebug(compiled,node);
			compiled.add(new BCStore(node));
			return compiled;
		}
		if (node instanceof GSAssignment) { // similar to end of GSInitialiser code
			checkType(node,0,GSIdentifierOrList.class);
			checkType(node,1,GSExpression.class);
			final ParseNode target = node.child(0).child(0);
			// this may be a NORMAL VARIABLE (GSIdentifier) or a SPECIFIC LIST ELEMENT (GSIdentifierWithIndex)
			if (target.getClass().equals(GSIdentifier.class)) {
				// assign/varname/value
				compiled.addAll(compile(node.child(1)));
				compiled.add(new BCString(node.child(0),node.child(0).tokens()));
				addDebug(compiled, node);
				compiled.add(new BCStore(node));
				return compiled;
			}
			if (target.getClass().equals(GSIdentifierWithIndex.class)) {
				//assignelement/varname/elementno/value
				compiled.addAll(compile(node.child(1))); // the value
				compiled.addAll(compile(target.child(1))); // evaluate index onto stack
				compiled.add(new BCString(target.child(0),target.child(0).tokens())); // push name
				addDebug(compiled, node);
				compiled.add(new BCStoreIndexed(node));
				return compiled;
			}
			throw new SystemImplementationException("Compiler error: Unknown type of Assignment: "+target.getClass().getName());
		}

		if (node instanceof GSStringConstant) {
			String string=node.tokens();
			string=string.substring(1,string.length()-1);
			compiled.add(new BCString(node,string));
			return compiled;
		}

		if (node instanceof GSIntegerConstant) {
			compiled.add(new BCInteger(node,node.tokens()));
			return compiled;
		}


		if (node instanceof GSConditional) {
			// conditional branch
			checkType(node,0,GSExpression.class);
			checkType(node,1,GSStatement.class);
			// evaluate the condition, branch if zero, otherwise run the statement
			compiled.addAll(compile(node.child(0))); // compile condition
			final BCLabel posttruth=new BCLabel(node,jumpnumber++);
			final BCLabel postfalse=new BCLabel(node,jumpnumber++);
			addDebug(compiled,node);
			compiled.add(new BCBranchIfZero(node,posttruth)); // if false, branch to posttruth
			compiled.addAll(compile(node.child(1))); // truth
			compiled.add(new BCInteger(node,0)); // if we're still here (in truth)
			compiled.add(new BCBranchIfZero(node,postfalse)); // branch to end of whole statement
			compiled.add(posttruth); // where we go if false
			if (node.children()==3) { compiled.addAll(compile(node.child(2))); }
			compiled.add(postfalse); // end of the whole thing
			return compiled;
		}

		if (node instanceof GSWhileLoop) {
			checkType(node,0,GSExpression.class);
			checkType(node,1,GSStatement.class);
			final BCLabel start=new BCLabel(node,jumpnumber++);
			final BCLabel end=new BCLabel(node,jumpnumber++);
			// check condition, exit if false, code block, repeat
			addDebug(compiled,node);
			compiled.add(start);
			compiled.addAll(compile(node.child(0)));
			compiled.add(new BCBranchIfZero(node,end));
			compiled.addAll(compile(node.child(1)));
			compiled.add(new BCInteger(node,0));
			compiled.add(new BCBranchIfZero(node,start));
			compiled.add(end);
			return compiled;
		}

		if (node instanceof GSFunctionCall) {
			// lots of random glue lives in here, but actually the function call at this level is easy enough, it has a name and some parameters
			checkType(node, 0, GSFunctionName.class);
			if (node.jjtGetNumChildren()>1) { checkType(node, 1, GSParameters.class); }
			// validate the function name
			final String functionname = node.child(0).tokens();
			if (!validFunction(functionname)) {
				throw new GSUnknownIdentifier("Function " + functionname + " does not exist");
			}
			// dump the paramters, in reverse order, (which starts with the paramter count), and finally the name and the invoking bytecode
			if (node.jjtGetNumChildren()>1) { compiled.addAll(compile(node.child(1))); } // assuming it has parameters
			else { compiled.add(new BCInteger(node,0)); } // else zero parameters
			compiled.add(new BCString(node,(node.child(0).tokens())));
			addDebug(compiled,node);
			compiled.add(new BCInvoke(node));
			return compiled;
		}

		if (node instanceof GSParameters) {
			for (int i=node.jjtGetNumChildren()-1;i>=0;i--) {
				compiled.addAll(compile(node.child(i)));
			}
			compiled.add(new BCInteger(node,node.children()));
			return compiled;
		}

		if (node instanceof GSBinaryOperation) {
			// term op expression
			checkType(node,0,GSTerm.class);
			checkType(node,1,GSBinaryOperator.class);
			checkType(node,2,GSExpression.class);
			// exection is just OP pops 2 and pushes result so....
			compiled.addAll(compile(node.child(2)));  // reverse polish
			compiled.addAll(compile(node.child(0)));  // so first term is pushed last!
			boolean handledop=false;
			final String op=node.child(1).tokens();
			//"+" | "-" | "*" | "/" | "==" | "!="
			addDebug(compiled,node);
			if (op.equals("+")) { handledop=true; compiled.add(new BCAdd(node.child(1))); }
			if (op.equals("-")) { handledop=true; compiled.add(new BCSubtract(node.child(1))); }
			if (op.equals("*")) { handledop=true; compiled.add(new BCMultiply(node.child(1))); }
			if (op.equals("/")) { handledop=true; compiled.add(new BCDivide(node.child(1))); }
			if (op.equals("==")) { handledop=true; compiled.add(new BCEquality(node.child(1))); }
			if (op.equals("!=")) { handledop=true; compiled.add(new BCInequality(node.child(1))); }
			if (op.equals(">")) { handledop=true; compiled.add(new BCGreaterThan(node.child(1))); }
			if (op.equals("<")) { handledop=true; compiled.add(new BCLessThan(node.child(1))); }
			if (op.equals(">=")) { handledop=true; compiled.add(new BCGreaterThanEqual(node.child(1))); }
			if (op.equals("<=")) { handledop=true; compiled.add(new BCLessThanEqual(node.child(1))); }
			return compiled;
		}

		if (node instanceof GSIdentifier) {
			// pull the variable onto the stack.  Kinda
			compiled.add(new BCString(node,node.tokens()));
			addDebug(compiled,node);
			compiled.add(new BCLoad(node));
			return compiled;
		}

		if (node instanceof GSList) {
			// pop the list, in reverse order, then short the size, and then the command.
			for (int i=node.children()-1;i>=0;i--) {
				compiled.addAll(compile(node.child(i)));
			}
			compiled.add(new BCList(node,node.children()));
			return compiled;
		}

		if (node instanceof GSListIndex) { // a list index in an evaluatable position
			checkType(node,0,GSIdentifier.class);
			checkType(node,1,GSExpression.class);
			// pop name, pop index
			compiled.addAll(compile(node.child(1)));
			compiled.add(new BCString(node.child(0),node.child(0).tokens()));
			addDebug(compiled,node);
			compiled.add(new BCLoadIndexed(node));
			return compiled;
		}

		throw new SystemImplementationException("Compilation not implemented for node type '"+node.getClass().getSimpleName()+"'");
	}

	private void checkType(@Nonnull final ParseNode node, final int pos, @Nonnull final Class<? extends ParseNode> clazz) {
		if (node.jjtGetNumChildren()<pos) { throw new GSInternalError("Checking type failed = has "+node.jjtGetNumChildren()+" and we asked for pos_0: "+pos+" in clazz "+clazz.getName()); }
		final Node child=node.jjtGetChild(pos);
		if (!child.getClass().equals(clazz)) { throw new GSInternalError("Child_0 "+pos+" of "+node.getClass().getName()+" is of type "+child.getClass().getName()+" not the expected "+clazz.getName()); }
	}

	private boolean validFunction(final String name) {
		for (final String funname: GSFunctions.getAll().keySet()) {
			if (funname.equals(name)) { return true; }
		}
		return false;
	}


}
