package com.liferay.support.tools.user;

import com.liferay.exportimport.kernel.configuration.ExportImportConfigurationConstants;
import com.liferay.exportimport.kernel.configuration.ExportImportConfigurationSettingsMapFactoryUtil;
import com.liferay.exportimport.kernel.lar.PortletDataHandlerKeys;
import com.liferay.exportimport.kernel.model.ExportImportConfiguration;
import com.liferay.exportimport.kernel.service.ExportImportConfigurationLocalService;
import com.liferay.exportimport.kernel.service.ExportImportConfigurationLocalServiceUtil;
import com.liferay.exportimport.kernel.service.ExportImportLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.model.LayoutTemplate;
import com.liferay.portal.kernel.model.LayoutTypePortlet;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.access.control.AccessControlled;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.transaction.Isolation;
import com.liferay.portal.kernel.transaction.Transactional;
import com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.common.PropsValues;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.annotation.ProviderType;

/**
 * User Layout Utilities
 * <p/>
 * The methods here are private or not exposed in liferay-impl, so simply copied methods over here.
 *
 * @author Yasuyuki Takeo
 * @see com.liferay.portal.events.ServicePreAction
 */
@AccessControlled
@Component(service = UserLayoutUtil.class)
@ProviderType
@Transactional(
    isolation = Isolation.PORTAL,
    rollbackFor = {
        PortalException.class,
        SystemException.class
    }
)
public class UserLayoutUtil {
    /**
     * Generating private / public page for a user (MyPage and Dashboard)
     *
     * @param user
     * @throws Exception
     */
	public void updateUserLayouts(User user) throws Exception {
		Boolean hasPowerUserRole = null;

		// Private layouts

		boolean addDefaultUserPrivateLayouts = false;

		if (PropsValues.LAYOUT_USER_PRIVATE_LAYOUTS_ENABLED &&
			PropsValues.LAYOUT_USER_PRIVATE_LAYOUTS_AUTO_CREATE) {

			addDefaultUserPrivateLayouts = true;

			if (PropsValues.LAYOUT_USER_PRIVATE_LAYOUTS_POWER_USER_REQUIRED) {
				if (hasPowerUserRole == null) {
					hasPowerUserRole = hasPowerUserRole(user);
				}

				if (!hasPowerUserRole.booleanValue()) {
					addDefaultUserPrivateLayouts = false;
				}
			}
		}

		Boolean hasPrivateLayouts = null;

		if (addDefaultUserPrivateLayouts) {
			hasPrivateLayouts = LayoutLocalServiceUtil.hasLayouts(
				user, true, false);

			if (!hasPrivateLayouts) {
				addDefaultUserPrivateLayouts(user);
			}
		}

		boolean deleteDefaultUserPrivateLayouts = false;

		if (!PropsValues.LAYOUT_USER_PRIVATE_LAYOUTS_ENABLED) {
			deleteDefaultUserPrivateLayouts = true;
		}
		else if (PropsValues.LAYOUT_USER_PRIVATE_LAYOUTS_POWER_USER_REQUIRED) {
			if (hasPowerUserRole == null) {
				hasPowerUserRole = hasPowerUserRole(user);
			}

			if (!hasPowerUserRole.booleanValue()) {
				deleteDefaultUserPrivateLayouts = true;
			}
		}

		if (deleteDefaultUserPrivateLayouts) {
			if (hasPrivateLayouts == null) {
				hasPrivateLayouts = LayoutLocalServiceUtil.hasLayouts(
					user, true, false);
			}

			if (hasPrivateLayouts) {
				deleteDefaultUserPrivateLayouts(user);
			}
		}

		// Public pages

		boolean addDefaultUserPublicLayouts = false;

		if (PropsValues.LAYOUT_USER_PUBLIC_LAYOUTS_ENABLED &&
			PropsValues.LAYOUT_USER_PUBLIC_LAYOUTS_AUTO_CREATE) {

			addDefaultUserPublicLayouts = true;

			if (PropsValues.LAYOUT_USER_PUBLIC_LAYOUTS_POWER_USER_REQUIRED) {
				if (hasPowerUserRole == null) {
					hasPowerUserRole = hasPowerUserRole(user);
				}

				if (!hasPowerUserRole.booleanValue()) {
					addDefaultUserPublicLayouts = false;
				}
			}
		}

		Boolean hasPublicLayouts = null;

		if (addDefaultUserPublicLayouts) {
			hasPublicLayouts = LayoutLocalServiceUtil.hasLayouts(
				user, false, false);

			if (!hasPublicLayouts) {
				addDefaultUserPublicLayouts(user);
			}
		}

		boolean deleteDefaultUserPublicLayouts = false;

		if (!PropsValues.LAYOUT_USER_PUBLIC_LAYOUTS_ENABLED) {
			deleteDefaultUserPublicLayouts = true;
		}
		else if (PropsValues.LAYOUT_USER_PUBLIC_LAYOUTS_POWER_USER_REQUIRED) {
			if (hasPowerUserRole == null) {
				hasPowerUserRole = hasPowerUserRole(user);
			}

			if (!hasPowerUserRole.booleanValue()) {
				deleteDefaultUserPublicLayouts = true;
			}
		}

		if (deleteDefaultUserPublicLayouts) {
			if (hasPublicLayouts == null) {
				hasPublicLayouts = LayoutLocalServiceUtil.hasLayouts(
					user, false, false);
			}

			if (hasPublicLayouts) {
				deleteDefaultUserPublicLayouts(user);
			}
		}
	}

    protected void deleteDefaultUserPrivateLayouts(User user)
        throws PortalException {

        Group group = user.getGroup();

        ServiceContext serviceContext = new ServiceContext();

        _layoutLocalService.deleteLayouts(
            group.getGroupId(), true, serviceContext);
    }

    protected void addDefaultUserPrivateLayouts(User user)
        throws PortalException {

        Group group = user.getGroup();

        if (privateLARFile != null) {
            addDefaultLayoutsByLAR(
                user.getUserId(), group.getGroupId(), true, privateLARFile);
        } else {
            addDefaultUserPrivateLayoutByProperties(
                user.getUserId(), group.getGroupId());
        }
    }

    protected void addDefaultUserPrivateLayoutByProperties(
        long userId, long groupId)
        throws PortalException {

        String friendlyURL = getFriendlyURL(
            PropsValues.DEFAULT_USER_PRIVATE_LAYOUT_FRIENDLY_URL);

        ServiceContext serviceContext = new ServiceContext();

        Layout layout = LayoutLocalServiceUtil.addLayout(
            userId, groupId, true, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID,
            PropsValues.DEFAULT_USER_PRIVATE_LAYOUT_NAME, StringPool.BLANK,
            StringPool.BLANK, LayoutConstants.TYPE_PORTLET, false, friendlyURL,
            serviceContext);

        LayoutTypePortlet layoutTypePortlet =
            (LayoutTypePortlet) layout.getLayoutType();

        layoutTypePortlet.setLayoutTemplateId(
            0, PropsValues.DEFAULT_USER_PRIVATE_LAYOUT_TEMPLATE_ID, false);

        LayoutTemplate layoutTemplate = layoutTypePortlet.getLayoutTemplate();

        for (String columnId : layoutTemplate.getColumns()) {
            String keyPrefix = PropsKeys.DEFAULT_USER_PRIVATE_LAYOUT_PREFIX;

            String portletIds = PropsUtil.get(keyPrefix.concat(columnId));

            layoutTypePortlet.addPortletIds(
                0, StringUtil.split(portletIds), columnId, false);
        }

        _layoutLocalService.updateLayout(
            layout.getGroupId(), layout.isPrivateLayout(), layout.getLayoutId(),
            layout.getTypeSettings());

        boolean updateLayoutSet = false;

        LayoutSet layoutSet = layout.getLayoutSet();

        if (Validator.isNotNull(
            PropsValues.DEFAULT_USER_PRIVATE_LAYOUT_REGULAR_THEME_ID)) {

            layoutSet.setThemeId(
                PropsValues.DEFAULT_USER_PRIVATE_LAYOUT_REGULAR_THEME_ID);

            updateLayoutSet = true;
        }

        if (Validator.isNotNull(
            PropsValues.DEFAULT_USER_PRIVATE_LAYOUT_REGULAR_COLOR_SCHEME_ID)) {

            layoutSet.setColorSchemeId(
                PropsValues.DEFAULT_USER_PRIVATE_LAYOUT_REGULAR_COLOR_SCHEME_ID);

            updateLayoutSet = true;
        }

        if (updateLayoutSet) {
            _LayoutSetLocalService.updateLayoutSet(layoutSet);
        }
    }

    /**
     * Has PowerUser role
     *
     * @param user
     * @return
     * @throws Exception
     */
    protected Boolean hasPowerUserRole(User user) throws Exception {
        return _roleLocalService.hasUserRole(
            user.getUserId(), user.getCompanyId(), RoleConstants.POWER_USER,
            true);
    }

    /**
     * Delete DefaultUser Public Layouts
     *
     * @param user
     * @throws PortalException
     */
    protected void deleteDefaultUserPublicLayouts(User user)
        throws PortalException {

        Group userGroup = user.getGroup();

        ServiceContext serviceContext = new ServiceContext();

        _layoutLocalService.deleteLayouts(
            userGroup.getGroupId(), false, serviceContext);
    }

    protected void addDefaultUserPublicLayouts(User user)
        throws PortalException {

        Group userGroup = user.getGroup();

        if (publicLARFile != null) {
            addDefaultLayoutsByLAR(
                user.getUserId(), userGroup.getGroupId(), false, publicLARFile);
        } else {
            addDefaultUserPublicLayoutByProperties(
                user.getUserId(), userGroup.getGroupId());
        }
    }

	protected void addDefaultLayoutsByLAR(
			long userId, long groupId, boolean privateLayout, File larFile)
		throws PortalException {

		User user = _userLocalService.getUser(userId);

		Map<String, String[]> parameterMap = new HashMap<>();

		parameterMap.put(
			PortletDataHandlerKeys.PERMISSIONS,
			new String[] {Boolean.TRUE.toString()});
		parameterMap.put(
			PortletDataHandlerKeys.PORTLET_ARCHIVED_SETUPS_ALL,
			new String[] {Boolean.TRUE.toString()});
		parameterMap.put(
			PortletDataHandlerKeys.PORTLET_CONFIGURATION,
			new String[] {Boolean.TRUE.toString()});
		parameterMap.put(
			PortletDataHandlerKeys.PORTLET_CONFIGURATION_ALL,
			new String[] {Boolean.TRUE.toString()});
		parameterMap.put(
			PortletDataHandlerKeys.PORTLET_DATA,
			new String[] {Boolean.TRUE.toString()});
		parameterMap.put(
			PortletDataHandlerKeys.PORTLET_DATA_ALL,
			new String[] {Boolean.TRUE.toString()});
		parameterMap.put(
			PortletDataHandlerKeys.PORTLET_DATA_CONTROL_DEFAULT,
			new String[] {Boolean.TRUE.toString()});
		parameterMap.put(
			PortletDataHandlerKeys.PORTLET_SETUP_ALL,
			new String[] {Boolean.TRUE.toString()});
		parameterMap.put(
			PortletDataHandlerKeys.PORTLET_USER_PREFERENCES_ALL,
			new String[] {Boolean.TRUE.toString()});
		parameterMap.put(
			PortletDataHandlerKeys.THEME_REFERENCE,
			new String[] {Boolean.TRUE.toString()});

		Map<String, Serializable> importLayoutSettingsMap =
			ExportImportConfigurationSettingsMapFactoryUtil.
				buildImportLayoutSettingsMap(
					user, groupId, privateLayout, null, parameterMap);

		ExportImportConfiguration exportImportConfiguration =
			ExportImportConfigurationLocalServiceUtil.
				addDraftExportImportConfiguration(
					user.getUserId(),
					ExportImportConfigurationConstants.TYPE_IMPORT_LAYOUT,
					importLayoutSettingsMap);

		_exportImportLocalService.importLayouts(
			exportImportConfiguration, larFile);
	}
	
    protected void addDefaultUserPublicLayoutByProperties(
        long userId, long groupId)
        throws PortalException {

        String friendlyURL = getFriendlyURL(
            PropsValues.DEFAULT_USER_PUBLIC_LAYOUT_FRIENDLY_URL);

        ServiceContext serviceContext = new ServiceContext();

        Layout layout = LayoutLocalServiceUtil.addLayout(
            userId, groupId, false, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID,
            PropsValues.DEFAULT_USER_PUBLIC_LAYOUT_NAME, StringPool.BLANK,
            StringPool.BLANK, LayoutConstants.TYPE_PORTLET, false, friendlyURL,
            serviceContext);

        LayoutTypePortlet layoutTypePortlet =
            (LayoutTypePortlet) layout.getLayoutType();

        layoutTypePortlet.setLayoutTemplateId(
            0, PropsValues.DEFAULT_USER_PUBLIC_LAYOUT_TEMPLATE_ID, false);

        LayoutTemplate layoutTemplate = layoutTypePortlet.getLayoutTemplate();

        for (String columnId : layoutTemplate.getColumns()) {
            String keyPrefix = PropsKeys.DEFAULT_USER_PUBLIC_LAYOUT_PREFIX;

            String portletIds = PropsUtil.get(keyPrefix.concat(columnId));

            layoutTypePortlet.addPortletIds(
                0, StringUtil.split(portletIds), columnId, false);
        }

        LayoutLocalServiceUtil.updateLayout(
            layout.getGroupId(), layout.isPrivateLayout(), layout.getLayoutId(),
            layout.getTypeSettings());

        boolean updateLayoutSet = false;

        LayoutSet layoutSet = layout.getLayoutSet();

        if (Validator.isNotNull(
            PropsValues.DEFAULT_USER_PUBLIC_LAYOUT_REGULAR_THEME_ID)) {

            layoutSet.setThemeId(
                PropsValues.DEFAULT_USER_PUBLIC_LAYOUT_REGULAR_THEME_ID);

            updateLayoutSet = true;
        }

        if (Validator.isNotNull(
            PropsValues.DEFAULT_USER_PUBLIC_LAYOUT_REGULAR_COLOR_SCHEME_ID)) {

            layoutSet.setColorSchemeId(
                PropsValues.DEFAULT_USER_PUBLIC_LAYOUT_REGULAR_COLOR_SCHEME_ID);

            updateLayoutSet = true;
        }

        if (updateLayoutSet) {
            _LayoutSetLocalService.updateLayoutSet(layoutSet);
        }
    }

    protected String getFriendlyURL(String friendlyURL) {
        friendlyURL = GetterUtil.getString(friendlyURL);

        return FriendlyURLNormalizerUtil.normalize(friendlyURL);
    }

    @Reference
    private RoleLocalService _roleLocalService;

    @Reference
    private LayoutLocalService _layoutLocalService;

    @Reference
    private LayoutSetLocalService _LayoutSetLocalService;

    @Reference
    private UserLocalService _userLocalService;

    @Reference
    private ExportImportConfigurationLocalService _exportImportConfigurationLocalService;

    @Reference
    private ExportImportLocalService _exportImportLocalService;

    protected File privateLARFile;
    protected File publicLARFile;


}
