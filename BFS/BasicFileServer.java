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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
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
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import BFS.net.HttpMeta;


public final class BasicFileServer implements ThreadFactory, AutoCloseable {

	private final Properties base;

	private final int size;
	private final int port;
	private final int blog;

	private final ExecutorService pool;


	private BasicFileServer(
		Properties config
	) throws IOException {
		base = config == null ? config(".arachnid") : config;

		port = Integer.parseInt(base.getProperty("port", "80"));
		size = Integer.parseInt(base.getProperty("size", "32"));
		blog = Integer.parseInt(base.getProperty("blog", "16"));

		pool = Executors.newFixedThreadPool(size, this);
	}


	private static synchronized void dump(ServerSocket socket, Properties config, String... params) {
		System.err.println(String.format("%s#%s @ %s:%s",
			socket.getClass().getCanonicalName(),
			System.identityHashCode(socket),
			socket.getInetAddress(),
			socket.getLocalPort()));
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
		ClassLoader app = BasicFileServer.class.getClassLoader();
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
			BasicFileServer svc = new BasicFileServer(config);
			ServerSocket tcp = new ServerSocket(svc.port, svc.blog);
		) {
			dump(tcp, config = svc.base, "root", "goto");

			while (!Thread.currentThread().isInterrupted()) {
				svc.handle(tcp.accept());
			}
		}
	}

	private void handle(Socket client) {
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
		BasicFileServer.listen();
	}


	private static class Swim implements HttpMeta, Runnable {

		private final Path home;
		private final Socket socket;

		private final List<String> request;
		private final List<String> response;
		private final URI redirect;
		private boolean session = false;


		private Swim(Properties config, Socket socket) {
			this.home = (Path)config.get("root");
			this.socket = socket;
			this.request = new ArrayList<>();
			this.response = new ArrayList<>();
			this.redirect = (URI)config.get("goto");
		}


		private static void copy(InputStream in, OutputStream out) throws IOException {
			byte[] block = new byte[4096];
			int count;

			while ((count = in.read(block)) > 0) {
				out.write(block, 0, count);
			}

			out.flush();
		}

		private static String examine(String path) {
			path = Objects.toString(path, "/").trim();

			return !path.isEmpty() && (
				path.charAt(0) == '/' ||
				path.charAt(0) == '\\' ||
				path.indexOf(':') >= 0 ||
				path.indexOf("..") >= 0
			) ? null : path;
		}

		private static synchronized void dump(Socket socket, List<String> bucket, String label) {
			System.err.println(String.format("%s#%s @ %s:%s",
				socket.getClass().getCanonicalName(),
				System.identityHashCode(socket),
				socket.getInetAddress(),
				socket.getPort()));
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

		private boolean process(InputStream in) throws IOException {
			return process(new BufferedReader(new InputStreamReader(in)));
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

		private boolean process(OutputStream out) throws IOException {
			final Path path = process(new BufferedWriter(new OutputStreamWriter(out)));

			if (path == null) {
			} else if (Files.isDirectory(path)) {
				final BasicFileAttributes[] attrs = { null };

				try (Stream<Path> list = Files.find(path, 1, (node, info) ->
					path != node && (attrs[0] = info) != null && (info.isDirectory() || info.isRegularFile())
				)) {
					Path item;
					Iterator<Path> iter = list.iterator();

					while (iter.hasNext()) {
						item = iter.next();

						Object file = item.getFileName();
						Object time = ISO_INSTANT.format(attrs[0].lastModifiedTime().toInstant());
						Object size = attrs[0].isDirectory() ? "-" : attrs[0].size();

						byte[] data = String
							.format("%s\t%s\t%s\r\n\r\n", file, time, size)
							.getBytes(UTF8);
						byte[] meta = String
							.format("%x\r\n", data.length - 2)
							.getBytes();

						out.write(meta);
						out.write(data);
						out.flush();
					}

					out.write(EMPTY_CHUNK);
					out.flush();
				}
			} else {
				try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
					copy(in, out);
				}
			}

			return !response.isEmpty();
		}

		private Path process(BufferedWriter out) throws IOException {
			Path pivot = null;
			URI hatch = null;

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
								response.clear();
								append(response, STATUS_LINE.print(resVer, Status.CODE_402));
							} else if (redirect == null) {
								pivot = home.resolve(URLDecoder.decode(reqUrl, UTF8));

								if (Files.isDirectory(pivot)) {
									append(response, STATUS_LINE.print(resVer, Status.CODE_200));
									append(response, TRANSFER_INFO.print(Transfer.ENCODING, "chunked"));
								} else if (Files.isReadable(pivot)) {
									append(response, STATUS_LINE.print(resVer, Status.CODE_200));
									append(response, CONTENT_INFO.print(Content.LENGTH, Files.size(pivot)));

									if (reqUrl.endsWith(".gz") || reqUrl.endsWith(".jgz")) {
										append(response, CONTENT_INFO.print(Content.ENCODING, "gzip"));
									}
								} else {
									append(response, STATUS_LINE.print(resVer, Files.exists(pivot)
										? Status.CODE_403
										: Status.CODE_404));

									pivot = null;
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

			return pivot;
		}

		private boolean iterate() throws IOException {
			return clear()
				&& process(socket.getInputStream())
				&& process(socket.getOutputStream());
		}


		@Override
		public void run() {
			try (Socket task = socket) {
				while (iterate() && session);
			} catch (IOException e) {
			}
		}

	}

}