package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.Core.Exceptions.System.SystemLookupFailureException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class ObjectType {

	final State state;
	@Nonnull
	final ObjType object;
	@Nonnull
	final JSONObject json;

	protected ObjectType(final State st,
	                     @Nonnull final ObjType object) {
		state=st;
		this.object=object;
		json=object.getBehaviour();
	}

	// ---------- STATICS ----------
	@Nonnull
	public static ObjectType materialise(final State st,
	                                     @Nonnull final ObjType object) {
		final JSONObject json=object.getBehaviour();
		final String behaviour=json.optString("behaviour","");
		if (behaviour.equals("ClickTeleport")) { return new ClickTeleporter(st,object); }
		if (behaviour.equals("PhantomTeleport")) { return new PhantomTeleporter(st,object); }
		if (behaviour.equals("RunCommand")) { return new RunCommand(st,object); }
		if (behaviour.equals("NPC")) { return new NPC(st,object); }
		throw new SystemLookupFailureException("Behaviour "+behaviour+" is not known!");
	}

	@Nonnull
	public static Map<String,String> getObjectTypes(final State st) {
		final Map<String,String> options=new TreeMap<>();
		options.put("ClickTeleport","Teleport user on click.");
		options.put("PhantomTeleport","Teleport user on collision; becomes phantom.");
		options.put("RunCommand","Causes the character to run a command when they click.");
		options.put("NPC","Assigns a character to this object and allows it to participate in scripted events");
		return options;
	}

	@Nonnull
	public static DropDownList getDropDownList(final State st) {
		final DropDownList behaviours=new DropDownList("behaviour");
		final Map<String,String> types=getObjectTypes(st);
		for (final Map.Entry<String,String> entry: types.entrySet()) {
			behaviours.add(entry.getKey(),entry.getValue());
		}
		return behaviours;
	}

    public static Set<String> getObjectTypesSet() {
		Set<String> types=new HashSet<>();
		types.add("ClickTeleport");
		types.add("PhantomTeleport");
		types.add("RunCommand");
		types.add("NPC");
		return types;
    }

    // ---------- INSTANCE ----------
	@Nonnull
	public abstract String explainHtml();

	public abstract void editForm(State st);

	public abstract void update(State st);

	@Nonnull
	public abstract String explainText();

	public void payload(final State st,
	                    @Nonnull final JSONObject response,
	                    @Nonnull final Region region,
	                    @Nullable final String url) {
		response.put("mode",mode());
	}

	@Nonnull
	public abstract MODE mode();

	@Nonnull
	public Response click(final State st,
	                      final Char clicker) { return new ErrorResponse("Object type "+object.getName()+" does not support click behaviour"); }

	@Nonnull
	public Response collide(final State st,
	                        final Char collider) { return new ErrorResponse("Object type "+object.getName()+" does not support collision behaviour"); }

	protected void populateVmVariables(State st, GSVM vm) {
		vm.introduce("OBJECTNAME",new BCString(null,st.object.getName()));
		vm.introduce("OBJECTTYPE",new BCString(null,st.object.getObjectType().getName()));
		vm.introduce("OBJECTKEY",new BCString(null,st.object.getUUID()));

	}

	enum MODE {
		NONE,
		CLICKABLE,
		PHANTOM
	}
}
