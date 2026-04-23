package com.liferay.support.tools.it.container

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.nio.file.Path

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LiferayContainer {

	private static final Logger LOG = LoggerFactory.getLogger(LiferayContainer)

	static final int HTTP_PORT = 8080
	static final int GOGO_PORT = 11311
	static final int JACOCO_PORT = 6300
	static final String DEPLOY_DIR = '/opt/liferay/deploy/'
	static final String DEFAULT_ADMIN_EMAIL = 'test@liferay.com'
	static final String DEFAULT_ADMIN_PASSWORD = 'test'

	private static LiferayContainer INSTANCE

	final String host
	final int httpPort
	final int gogoPort
	final int jacocoPort
	final String containerName

	private LiferayContainer() {
		this.host = System.getProperty('liferay.host', 'localhost')
		this.httpPort = Integer.parseInt(
			System.getProperty('liferay.http.port', String.valueOf(HTTP_PORT)))
		this.gogoPort = Integer.parseInt(
			System.getProperty('liferay.gogo.port', String.valueOf(GOGO_PORT)))
		this.jacocoPort = Integer.parseInt(
			System.getProperty('liferay.jacoco.port', String.valueOf(JACOCO_PORT)))
		this.containerName = System.getProperty(
			'liferay.container.name', 'liferay-dummy-factory-liferay')
	}

	static synchronized LiferayContainer getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LiferayContainer()
		}
		return INSTANCE
	}

	String getBaseUrl() {
		return "http://${host}:${httpPort}"
	}

	boolean isRunning() {
		try {
			def conn = new URL("${baseUrl}/c/portal/login").openConnection()
			conn.connectTimeout = 2000
			conn.readTimeout = 2000
			conn.instanceFollowRedirects = false
			int code = conn.responseCode
			return code == 200 || code == 302
		}
		catch (ConnectException ignored) {
			return false
		}
		catch (SocketTimeoutException ignored) {
			return false
		}
		catch (IOException e) {
			LOG.warn("LiferayContainer.isRunning() unexpected IOException: {}", e.message)
			return false
		}
	}

	/**
	 * Deploy a JAR into the running container via `docker cp` + chown.
	 * Used by BaseLiferaySpec.ensureBundleActive() when a test needs to
	 * re-deploy between runs. For first-time deploy, the JAR is already
	 * baked into the image via dockerDeploy.
	 */
	void deployJar(Path jarPath) {
		String fileName = jarPath.fileName.toString()
		String tmpPath = "/tmp/${fileName}"
		String targetPath = DEPLOY_DIR + fileName
		_runDocker('cp', jarPath.toString(), "${containerName}:${tmpPath}")
		_runDocker('exec', '-u', '0', containerName, 'bash', '-c',
			"cp ${tmpPath} ${targetPath} && " +
				"chown liferay:liferay ${targetPath} && " +
				"rm ${tmpPath}")
	}

	private static String _runDocker(String... args) {
		def cmd = ['docker', *args]
		def proc = new ProcessBuilder(cmd).redirectErrorStream(true).start()
		String output = proc.inputStream.text
		int exit = proc.waitFor()
		if (exit != 0) {
			throw new IllegalStateException(
				"docker ${args.join(' ')} failed (exit=${exit}): ${output}")
		}
		return output
	}

}
