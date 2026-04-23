package com.liferay.support.tools.it.util

import org.apache.commons.net.telnet.TelnetClient

class GogoShellClient implements Closeable {

	private final TelnetClient telnet
	private final InputStream inputStream
	private final OutputStream outputStream

	GogoShellClient(String host, int port) {
		telnet = new TelnetClient()
		telnet.setConnectTimeout(5000)
		telnet.connect(host, port)
		inputStream = telnet.inputStream
		outputStream = telnet.outputStream
		readUntilPrompt()
	}

	String execute(String command) {
		outputStream.write((command + '\n').bytes)
		outputStream.flush()
		return readUntilPrompt()
	}

	private String readUntilPrompt() {
		StringBuilder sb = new StringBuilder()
		long deadline = System.currentTimeMillis() + 30_000

		while (System.currentTimeMillis() < deadline) {
			if (inputStream.available() > 0) {
				int ch = inputStream.read()

				if (ch == -1) {
					break
				}

				sb.append((char) ch)

				int len = sb.length()

				if (len >= 3 &&
					sb.charAt(len - 3) == (char) 'g' &&
					sb.charAt(len - 2) == (char) '!' &&
					sb.charAt(len - 1) == (char) ' ') {

					break
				}
			}
			else {
				Thread.sleep(100)
			}
		}

		return sb.toString()
	}

	@Override
	void close() {
		try { telnet.disconnect() } catch (ignored) { }
	}

}
