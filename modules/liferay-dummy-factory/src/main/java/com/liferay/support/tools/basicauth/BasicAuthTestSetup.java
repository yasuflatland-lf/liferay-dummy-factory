package com.liferay.support.tools.basicauth;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * Integration-test only. Re-enables {@code BasicAuthHeaderSupportConfiguration}
 * so BasicAuth-authenticated JSONWS calls from the Spock suite reach
 * {@code BasicAuthHeaderAutoLoginSupport}, which sets
 * {@code passwordBasedAuthentication=true} on the AuthVerifierResult — that
 * flag is what activates the {@code SYSTEM_USER_PASSWORD} SAP policy
 * downstream.
 *
 * DXP 2026 ships {@code portal-liferay-online-config.properties} inside the WAR
 * with:
 * <pre>
 * configuration.override.com.liferay.portal.security.configuration.BasicAuthHeaderSupportConfiguration_enabled=B"false"
 * </pre>
 * That file is processed after {@code portal-ext.properties}, so a user-supplied
 * {@code configuration.override.*} in portal-ext cannot win. The only reliable
 * fix is to write the configuration via {@link ConfigurationAdmin} after
 * {@code portal.initialized}, which is strictly after all property-file seeding.
 *
 * {@code BasicAuthHeaderAutoLoginSupport} reads the configuration freshly on
 * every request (via {@code ConfigurationProvider.getCompanyConfiguration}), so
 * no {@code @Modified} callback is needed and the override takes effect on the
 * next BasicAuth attempt.
 *
 * Gated by {@link ConfigurationPolicy#REQUIRE}: the component only activates
 * when an {@code osgi/configs/com.liferay.support.tools.basicauth.BasicAuthTestSetup.config}
 * file is present. Production deploys that do not ship this file are
 * unaffected.
 */
@Component(
	configurationPid = "com.liferay.support.tools.basicauth.BasicAuthTestSetup",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	immediate = true,
	service = {}
)
public class BasicAuthTestSetup {

	@Activate
	protected void activate() {
		_log.info("BasicAuthTestSetup activating — enabling BasicAuth");

		try {
			_enableConfig(
				"com.liferay.portal.security.configuration." +
					"BasicAuthHeaderSupportConfiguration");
		}
		catch (Exception exception) {
			_log.error(
				"Unable to enable BasicAuthHeaderSupportConfiguration",
				exception);
		}
	}

	private void _enableConfig(String pid) throws Exception {
		Configuration configuration = _configurationAdmin.getConfiguration(
			pid, "?");

		Dictionary<String, Object> existing = configuration.getProperties();

		Dictionary<String, Object> properties = new Hashtable<>();

		if (existing != null) {
			Enumeration<String> keys = existing.keys();

			while (keys.hasMoreElements()) {
				String key = keys.nextElement();

				properties.put(key, existing.get(key));
			}
		}

		properties.put("enabled", Boolean.TRUE);

		configuration.update(properties);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		BasicAuthTestSetup.class);

	@Reference
	private ConfigurationAdmin _configurationAdmin;

	@Reference(target = "(module.service.lifecycle=portal.initialized)")
	private ModuleServiceLifecycle _moduleServiceLifecycle;

}
