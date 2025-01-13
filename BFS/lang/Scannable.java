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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class Scannable {

	private final Pattern patty;
	private final Object[] ids;


	protected Scannable(
		String pattern,
		Object[] ids
	) {
		this.patty = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		this.ids = ids;
	}


	@SuppressWarnings({
		"rawtypes",
		"unchecked"
	})
	protected final boolean $(
		String text,
		Map cache
	) {
		final Matcher matty = patty.matcher(text);

		cache.clear();

		if (matty.matches()) {
			for (Object id : ids) {
				cache.put(id, matty.group(id.toString()));
			}

			return true;
		} else {
			return false;
		}
	}


	public static class TypedScanner<T> extends Scannable {

		public TypedScanner(T[] ids, String pattern) {
			super(pattern, ids);
		}

		public TypedScanner(Collection<T> ids, String pattern) {
			super(pattern, ids.toArray());
		}

		public final boolean scan(String text, Map<T, String> cache) {
			return $(text, cache);
		}

	}

}
