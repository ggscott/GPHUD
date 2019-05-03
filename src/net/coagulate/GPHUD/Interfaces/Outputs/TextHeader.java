package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import java.util.HashSet;
import java.util.Set;

/**
 * Implements a header element.
 * Hmm, this is bad, the content should just be an element, not forced text.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TextHeader implements Renderable {
	Renderable content;

	public TextHeader(String s) { content = new Text(s); }

	public TextHeader(Renderable r) { content = r; }

	@Override
	public String asText(State st) {
		return "===== " + content.asText(st) + " =====\n";
	}

	@Override
	public String asHtml(State st, boolean rich) {
		return "<h1>" + content.asHtml(st, rich) + "</h1>";
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		Set<Renderable> r = new HashSet<>();
		r.add(content);
		return r;
	}
}
