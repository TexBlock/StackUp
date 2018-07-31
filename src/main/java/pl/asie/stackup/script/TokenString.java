/*
 * Copyright (c) 2018 Adrian Siekierka
 *
 * This file is part of StackUp.
 *
 * StackUp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * StackUp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with StackUp.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.stackup.script;

import net.minecraft.item.Item;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.function.Function;
import java.util.function.Predicate;

public class TokenString<T> extends Token<T> {
	protected final Function<T, String> function;
	private final boolean ignoreCase;
	private ComparisonType type;
	private String s;

	public TokenString(Function<T, String> function, boolean ignoreCase) {
		this.function = function;
		this.ignoreCase = ignoreCase;
	}

	@Override
	public void parse(PushbackReader reader) throws IOException, TokenException {
		type = getComparisonType(reader);
		if (type != ComparisonType.EQUAL && type != ComparisonType.APPROXIMATELY_EQUAL && type != ComparisonType.NOT_EQUAL) {
			throw new TokenException("Unsupported comparison type " + type + "!");
		}

		StringBuilder builder = new StringBuilder();
		ScriptHandler.cutWhitespace(reader);
		int c = reader.read();
		if (c != '"') {
			throw new TokenException("Expected string beginning, " + c + " found!");
		}

		while ((c = reader.read()) != '"') {
			builder.appendCodePoint(c);
		}
		ScriptHandler.cutWhitespace(reader);
		s = builder.toString();
	}

	protected String getString() {
		return s;
	}

	protected ComparisonType getComparisonType() {
		return type;
	}

	protected boolean compare(String sReceived, String sSet) {
		switch (type) {
			case EQUAL:
			default:
				return ignoreCase ? sReceived.equalsIgnoreCase(sSet) : sReceived.equals(sSet);
			case APPROXIMATELY_EQUAL:
				boolean hasStartStar = sSet.startsWith("*");
				boolean hasEndStar = sSet.endsWith("*");
				if (hasStartStar && hasEndStar) {
					if (sSet.length() == 1) {
						return true;
					} else {
						return sReceived.toLowerCase().contains(sSet.substring(1, sSet.length() - 1).toLowerCase());
					}
				} else if (hasStartStar) {
					return sReceived.toLowerCase().endsWith(sSet.substring(1, sSet.length()).toLowerCase());
				} else if (hasEndStar) {
					return sReceived.toLowerCase().startsWith(sSet.substring(0, sSet.length() - 1).toLowerCase());
				} else {
					return sReceived.equalsIgnoreCase(sSet);
				}
			case NOT_EQUAL:
				return ignoreCase ? !sReceived.equalsIgnoreCase(sSet) : !sReceived.equals(sSet);
		}
	}

	@Override
	public boolean apply(T object) {
		String str = function.apply(object);
		return compare(str, s);
	}

}
