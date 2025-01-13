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
package BFS.net;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collection;

import BFS.lang.Castable;
import BFS.lang.EnumLike;
import BFS.lang.Listable;
import BFS.lang.Printable.TypedPrinter1;
import BFS.lang.Printable.TypedPrinter2;
import BFS.lang.Scannable.TypedScanner;


public interface HttpMeta {

	public static final TypedScanner<Entity> GET_ENTITY = new TypedScanner<>(Entity.list(), "^GET /(?<file>[^\\s]*) HTTP/(?<http>[^\\s]+)$");
	public static final TypedPrinter2<Version, Status> STATUS_LINE = new TypedPrinter2<>("HTTP/%s %s");
	public static final TypedPrinter1<URI> LOCATION = new TypedPrinter1<>("Location: %s");
	public static final TypedPrinter1<Persist> CONNECTION = new TypedPrinter1<>("Connection: %s");
	public static final TypedPrinter2<Content, Object> CONTENT_INFO = new TypedPrinter2<>("Content-%s: %s");
	public static final TypedPrinter2<Transfer, String> TRANSFER_INFO = new TypedPrinter2<>("Transfer-%s: %s");

	public static final Charset UTF8 = StandardCharsets.UTF_8;
	public static final String CRLF = "\r\n";
	public static final DateTimeFormatter ISO_INSTANT = new DateTimeFormatterBuilder().appendInstant(0).toFormatter();
	public static final byte[] EMPTY_CHUNK = "0\r\n\r\n".getBytes();


	public static final class Entity extends EnumLike implements Listable<Entity> {

		public static final Entity FILE = define("file");
		public static final Entity HTTP = define("http");

		public static Collection<Entity> list() {
			return values();
		}

	}

	public static final class Version extends EnumLike implements Castable<Version> {

		public static final Version SPEC_10 = define("1.0");
		public static final Version SPEC_11 = define("1.1");
		public static final Version SPEC_1X = define("1.x");

		public static Version cast(String tbd, Version def) {
			return lookup(tbd, def);
		}

	}

	public static final class Status extends EnumLike {

		public static final Status CODE_200 = define("200 OK");                    // HttpServletResponse.SC_OK
		public static final Status CODE_302 = define("302 Found");                 // HttpServletResponse.SC_FOUND
		public static final Status CODE_402 = define("402 Payment Required");      // HttpServletResponse.SC_PAYMENT_REQUIRED
		public static final Status CODE_403 = define("403 Forbidden");             // HttpServletResponse.SC_FORBIDDEN
		public static final Status CODE_404 = define("404 Not Found");             // HttpServletResponse.SC_NOT_FOUND
		public static final Status CODE_500 = define("500 Internal Server Error"); // HttpServletResponse.SC_INTERNAL_SERVER_ERROR

	}

	public static final class Persist extends EnumLike {

		public static final Persist KEEP_ALIVE = define("keep-alive");
		public static final Persist NO_PERSIST = define("close");

	}

	public static final class Content extends EnumLike {

		public static final Content TYPE = define("Type");
		public static final Content LENGTH = define("Length");
		public static final Content ENCODING = define("Encoding");

	}

	public static final class Transfer extends EnumLike {

		public static final Transfer ENCODING = define("Encoding");

	}

}
