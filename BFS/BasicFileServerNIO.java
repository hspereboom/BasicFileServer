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
package BFS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import BFS.net.HttpMeta;


public final class BasicFileServerNIO implements ThreadFactory, AutoCloseable {

	private final Properties base;

	private final int size;
	private final int port;
	private final int blog;

	private final ExecutorService pool;


	private BasicFileServerNIO(
		Properties config
	) throws IOException {
		base = config == null ? config(".arachnid") : config;

		port = Integer.parseInt(base.getProperty("port", "80"));
		size = Integer.parseInt(base.getProperty("size", "32"));
		blog = Integer.parseInt(base.getProperty("blog", "16"));

		pool = Executors.newFixedThreadPool(size, this);
	}


	private static synchronized void dump(ServerSocketChannel channel, Properties config, String... params) throws IOException {
		System.err.println(String.format("%s#%s @ %s",
			channel.getClass().getCanonicalName(),
			System.identityHashCode(channel),
			channel.getLocalAddress()));
		System.err.println("========================================================");

		for (String item : params) {
			System.err.println(item + " " + config.getOrDefault(item, "n/a"));
		}

		System.err.println();
	}

	private static synchronized Object pick(String $root, String $goto) {
		if (absent($goto)) {
			final File curr = new File(".");
			final Path path;

			if (absent($root)) {
				JFileChooser picker = new JFileChooser();
				picker.setDialogTitle("Choose the web-root location");
				picker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				picker.setMultiSelectionEnabled(false);
		 		picker.setDragEnabled(false);
		 		picker.setCurrentDirectory(curr);

				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					picker.updateUI();
					UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				} catch (ReflectiveOperationException|UnsupportedLookAndFeelException wtf) {
					;
				}

				path = (picker.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ?
					picker.getSelectedFile() : curr).toPath();
			} else {
				path = Paths.get($root.trim());
			}

			return path.toAbsolutePath();
		} else {
			return URI.create($goto.trim());
		}
	}

	private static boolean absent(String param) {
		return param == null || param.trim().isEmpty();
	}

	private static Properties config(String ini) throws IOException {
		ClassLoader app = BasicFileServerNIO.class.getClassLoader();
		Properties tmp = new Properties();

		try {
			tmp.load(Files.newInputStream(Paths.get(ini)));
		} catch (NoSuchFileException|FileNotFoundException e) {
			tmp.load(app.getResource(ini).openStream());
		}

		return config(tmp);
	}

	private static Properties config(Properties ini) {
		final Object union = pick(
			ini.getProperty("root"),
			ini.getProperty("goto"));

		ini.put(union != null ?
			union instanceof Path ? "root" :
			union instanceof URI ? "goto" :
			null : null, union);

		return ini;
	}

	public static void listen() throws IOException {
		listen(null);
	}

	public static void listen(Properties config) throws IOException {
		try (
			BasicFileServerNIO svc = new BasicFileServerNIO(config);
			ServerSocketChannel server = ServerSocketChannel.open();
			ServerSocket tcp = server.socket();
		) {
			tcp.setReuseAddress(true);
			tcp.bind(new InetSocketAddress(svc.port), svc.blog);

			dump(server, config = svc.base, "root", "goto");

			while (!Thread.currentThread().isInterrupted()) {
				svc.handle(server.accept());
			}
		}
	}

	private void handle(SocketChannel client) {
		pool.submit(new Swim(base, client));
	}


	@Override
	public Thread newThread(Runnable task) {
		Thread thread = Executors.defaultThreadFactory().newThread(task);
		thread.setDaemon(true);
		return thread;
	}

	@Override
	public void close() {
		pool.shutdown();
	}


	public static void main(
		String... args
	) throws IOException {
		BasicFileServerNIO.listen();
	}


	private static class Swim implements HttpMeta, Runnable {

		private final Path home;
		private final SocketChannel socket;

		private final List<String> request;
		private final List<String> response;
		private final URI redirect;
		private boolean session = false;


		private Swim(Properties config, SocketChannel socket) {
			this.home = (Path)config.get("root");
			this.socket = socket;
			this.request = new ArrayList<>();
			this.response = new ArrayList<>();
			this.redirect = (URI)config.get("goto");
		}


		private static void copy(FileChannel in, WritableByteChannel out) throws IOException {
			in.transferTo(0, in.size(), out);
		}

		private static String examine(String path) {
			path = Objects.toString(path, "/").trim();

			return !path.isEmpty() && (
				path.charAt(0) == '/' ||
				path.indexOf(':') >= 0 ||
				path.indexOf("..") >= 0 ||
				path.indexOf("//") >= 0 ||
				path.indexOf('\\') >= 0
			) ? null : path;
		}

		private static int branch(Path path, String orig) {
			return path == null ? 0
				: Files.isDirectory(path) ? 1
				: Files.isReadable(path) ? 2 | (
					orig.endsWith("/") ? 1 :
					orig.endsWith(".gz") ? 4 :
					orig.endsWith(".jgz") ? 4 : 0)
				: 0;
		}

		private static Predicate<Path> filter1(Path base, BasicFileAttributes[] temp) throws IOException {
			final BasicFileAttributes info = Files.readAttributes(base, BasicFileAttributes.class);

			return node -> (temp[0] = info) != null
				&& (info.isDirectory() || info.isRegularFile());
		}

		private static BiPredicate<Path, BasicFileAttributes> filterN(Path base, BasicFileAttributes[] temp) {
			return (node, info) -> (temp[0] = info) != null
				&& (info.isDirectory() || info.isRegularFile());
		}

		private static synchronized void dump(SocketChannel channel, List<String> bucket, String label) throws IOException {
			System.err.println(String.format("%s#%s @ %s",
				channel.getClass().getCanonicalName(),
				System.identityHashCode(channel),
				channel.getRemoteAddress()));
			System.err.println("========================================================");

			for (String line : bucket) {
				System.err.println(label + " " + line);
			}

			System.err.println();
		}

		private static boolean append(List<String> bucket, String line) {
			return !line.isEmpty() && bucket.add(line);
		}


		private boolean clear() {
			request.clear();
			response.clear();
			session = false;

			return true;
		}

		private boolean process(ReadableByteChannel in) throws IOException {
			return process(new BufferedReader(Channels.newReader(in, UTF8)));
		}

		private boolean process(BufferedReader in) throws IOException {
			String line;

			do {
				if ((line = in.readLine()) == null) {
					throw new InterruptedIOException();
				}
			} while (append(request, line));

			dump(socket, request, "REQ");

			return !request.isEmpty();
		}

		private boolean process(WritableByteChannel out) throws IOException {
			final Entry<Path, Integer> job = process(new BufferedWriter(Channels.newWriter(socket, UTF8)));
			final Path pivot = job.getKey();
			final int flags = job.getValue();

			switch (flags) {
				case 1: case 3: case 7: {
					final OutputStream proxy = Channels.newOutputStream(out);
					final BasicFileAttributes[] attrs = { null };

					try (Stream<Path> list = (flags & 2) != 0
						? Stream.of(pivot).filter(filter1(pivot, attrs))
						: Files.find(pivot, 1, filterN(pivot, attrs))
					) {
						Path item;
						Iterator<Path> iter = list.iterator();

						while (iter.hasNext()) {
							item = iter.next();

							Object file = pivot.equals(item) ? "." : item.getFileName();
							Object time = ISO_INSTANT.format(attrs[0].lastModifiedTime().toInstant());
							Object size = attrs[0].isDirectory() ? "-" : attrs[0].size();

							byte[] data = String
								.format("%s\t%s\t%s\r\n\r\n", file, time, size)
								.getBytes(UTF8);
							byte[] meta = String
								.format("%x\r\n", data.length - 2)
								.getBytes();

							proxy.write(meta);
							proxy.write(data);
							proxy.flush();
						}

						proxy.write(EMPTY_CHUNK);
						proxy.flush();
					}
				}	break;

				case 2: case 6: {
					try (FileChannel in = FileChannel.open(pivot, StandardOpenOption.READ)) {
						copy(in, out);
					}
				}	break;
			}

			return !response.isEmpty();
		}

		private Entry<Path, Integer> process(BufferedWriter out) throws IOException {
			Path pivot = null;
			URI hatch = null;
			int flags = 0;

			try {
				if (redirect != null) {
					System.err.println("REDIRECT");
				}

				final String $persist = CONNECTION.print(Persist.KEEP_ALIVE);
				final Map<Entity, String> $entity = new HashMap<>();

				for (String line : request) {
					if (line.equalsIgnoreCase($persist)) {
						session = true;
					} else if (pivot == null && hatch == null) {
						if (GET_ENTITY.scan(line, $entity)) {
							final String reqUrl = examine($entity.get(Entity.FILE));
							final Version resVer = Version.cast($entity.get(Entity.HTTP), Version.SPEC_1X);

							if (reqUrl == null) {
								pivot = null;
								flags = 0;

								response.clear();
								append(response, STATUS_LINE.print(resVer, Status.CODE_402));
							} else if (redirect == null) {
								pivot = home.resolve(URLDecoder.decode(reqUrl, UTF8));
								flags = branch(pivot, reqUrl);

								switch (flags) {
									case 1: case 3: case 7:
										append(response, STATUS_LINE.print(resVer, Status.CODE_200));
										append(response, CONTENT_INFO.print(Content.TYPE, "text/plain; charset=utf-8"));
										append(response, TRANSFER_INFO.print(Transfer.ENCODING, "chunked"));
										break;
									case 6:
										append(response, CONTENT_INFO.print(Content.ENCODING, "gzip"));
									case 2:
										append(response, STATUS_LINE.print(resVer, Status.CODE_200));
										append(response, CONTENT_INFO.print(Content.LENGTH, Files.size(pivot)));
										break;
									default:
										flags = 0;
										append(response, STATUS_LINE.print(resVer, Files.exists(pivot)
											? Status.CODE_403
											: Status.CODE_404));
										break;
								}
							} else {
								hatch = redirect.resolve(reqUrl);

								append(response, STATUS_LINE.print(resVer, Status.CODE_302));
								append(response, LOCATION.print(hatch));
							}
						}
					}
				}
			} catch (IOException e) {
				response.clear();
				append(response, STATUS_LINE.print(Version.SPEC_1X, Status.CODE_500));
				pivot = null;
			} finally {
				dump(socket, response, "RES");

				for (String line : response) {
					out.write(line);
					out.write(CRLF);
				}

				out.write(CRLF);
				out.flush();
			}

			return Map.entry(pivot, flags);
		}

		private boolean iterate() throws IOException {
			return clear()
				&& process((ReadableByteChannel)socket)
				&& process((WritableByteChannel)socket);
		}


		@Override
		public void run() {
			try (SocketChannel task = socket) {
				while (iterate() && session);
			} catch (IOException e) {
			}
		}

	}

}
