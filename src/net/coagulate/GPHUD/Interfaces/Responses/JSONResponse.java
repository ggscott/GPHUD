package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * A raw response, unprocessed.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class JSONResponse implements Response {
	final JSONObject json;

	public JSONResponse(JSONObject j) {
		json = j;
	}

	@Override
	public JSONObject asJSON(State st) {
		return json;
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		return "<A JSON Response>";
	}

	@Nonnull
	@Override
	public String asText(State st) {
		throw new SystemException("JSONResponse can not be converted to Text");
	}

	@Nonnull
	@Override
	public String asHtml(State st, boolean rich) {
		throw new SystemException("JSONResponse can not be converted to HTML");
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		throw new SystemException("JSONResponse can not be interrogated as a Form");
	}
}
