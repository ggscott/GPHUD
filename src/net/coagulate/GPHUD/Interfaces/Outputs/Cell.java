package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * A cell in a table.
 * Basically an encapsulated Element, that is an element its self.
 *
 * @author iain
 */
public class Cell implements Renderable {

	@Nullable
	Renderable e=null;
	@Nonnull
	Renderable e() {
		if (e==null) { throw new SystemException("Cell content was null"); }
		return e;
	}
	boolean header = false;
	int colspan = 1;
	String align = "";

	public Cell() {}

	public Cell(String s) { e = new Text(s); }

	public Cell(@Nullable Renderable e) {
		if (e == null) { throw new SystemException("Abstract Cell is not renderable."); }
		this.e = e;
	}

	public Cell(String s, int colspan) {
		e = new Text(s);
		this.colspan = colspan;
	}

	public Cell(@Nullable Renderable e, int colspan) {
		if (e == null) {
			throw new SystemException("Abstract Cell is not renderable");
		}
		this.e = e;
		this.colspan = colspan;
	}

	@Nonnull
	@Override
	public String asText(State st) {
		if (header) { return "*" + e().asText(st) + "*"; }
		return e().asText(st);
	}

	@Nonnull
	@Override
	public String asHtml(State st, boolean rich) {
		String s = "";
		if (header) { s += "<th"; } else { s += "<td"; }
		if (colspan > 1) { s += " colspan=" + colspan; }
		if (!align.isEmpty()) { s += " align=" + align; }
		s += ">";
		s += e().asHtml(st, rich);
		s += "</";
		if (header) { s += "th>"; } else { s += "td>"; }
		return s;
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		Set<Renderable> r = new HashSet<>();
		r.add(e);
		return r;
	}

	@Nonnull
	public Cell th() {
		header = true;
		return this;
	}

	@Nonnull
	public Cell align(String align) {
		this.align = align;
		return this;
	}

}
