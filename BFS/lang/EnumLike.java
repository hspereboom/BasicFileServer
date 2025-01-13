/*
 * MIT License
 *
 * Copyright (C) 2020-2024 Harry Shungo Pereboom (github.com/hspereboom)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package BFS.lang;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * An enum-like structure meant to be extended.
 */
public abstract class EnumLike {

	private static final Map<Class<? extends EnumLike>, Set<EnumLike>> pile = new HashMap<>();
	private String text;


	protected EnumLike() {
	}


	@SuppressWarnings("unchecked")
	private static <T extends EnumLike> Class<T> let() {
		try {
			StackTraceElement[] cst = Thread.currentThread().getStackTrace();
			Class<?> cls = Class.forName(cst[3].getClassName());

			if (EnumLike.class.isAssignableFrom(cls)) {
				return (Class<T>)cls;
			}

			throw new IllegalAccessException();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends EnumLike> Set<T> let(
		Class<T> cls
	) {
		Set<EnumLike> enu = pile.get(cls);

		if (enu == null) {
			pile.put(cls, enu = new HashSet<>());
		}

		return (Set<T>)enu;
	}


	private synchronized static <T extends EnumLike> T set(
		Class<T> cls,
		T inst
	) {
		let(cls).add(inst);

		return inst;
	}

	@SuppressWarnings("unlikely-arg-type")
	private synchronized static <T extends EnumLike> T get(
		Class<T> cls,
		String text
	) {
		for (T inst : let(cls)) {
			if (inst.equals(text)) {
				return inst;
			}
		}

		return null;
	}


	protected static <T extends EnumLike> T define(
		String text
	) {
		text.hashCode();

		try {
			final Class<T> cls = let();
			final T inst = cls.getDeclaredConstructor().newInstance();

			EnumLike enu = inst;
			enu.text = text;

			return set(cls, inst);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e.getClass().getCanonicalName() + ": " + e.getMessage());
		}
	}

	protected static <T extends EnumLike & Castable<T>> T lookup(
		String text,
		T defau1t
	) {
		final Class<T> cls = let();
		final T inst = get(cls, text);

		return inst != null ? inst : defau1t;
	}

	protected static <T extends EnumLike & Listable<T>> Collection<T> values() {
		final Class<T> cls = let();
		final Set<T> enu = let(cls);

		return Collections.unmodifiableSet(enu);
	}


	@Override
	public final boolean equals(
		Object that
	) {
		return that == null ? false : that instanceof String ? text.equals(that) : this == that;
	}

	@Override
	public final String toString() {
		return text;
	}

}
