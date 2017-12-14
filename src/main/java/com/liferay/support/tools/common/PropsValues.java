package com.liferay.support.tools.common;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;

/**
 * PropsValues
 *
 * @see com.liferay.portal.util.PropsValues
 *
 * @author Yasuyuki Takeo
 */
public class PropsValues {
    public static final String DEFAULT_USER_PRIVATE_LAYOUT_FRIENDLY_URL = PropsUtil.get(PropsKeys.DEFAULT_USER_PRIVATE_LAYOUT_FRIENDLY_URL);
    public static final String DEFAULT_USER_PRIVATE_LAYOUT_NAME = PropsUtil.get(PropsKeys.DEFAULT_USER_PRIVATE_LAYOUT_NAME);
    public static final String DEFAULT_USER_PRIVATE_LAYOUT_TEMPLATE_ID = PropsUtil.get(PropsKeys.DEFAULT_USER_PRIVATE_LAYOUT_TEMPLATE_ID);
    public static final String DEFAULT_USER_PRIVATE_LAYOUT_REGULAR_THEME_ID = PropsUtil.get(PropsKeys.DEFAULT_USER_PRIVATE_LAYOUT_REGULAR_THEME_ID);
    public static final String DEFAULT_USER_PRIVATE_LAYOUT_REGULAR_COLOR_SCHEME_ID = PropsUtil.get(PropsKeys.DEFAULT_USER_PRIVATE_LAYOUT_REGULAR_COLOR_SCHEME_ID);
    public static final String DEFAULT_USER_PUBLIC_LAYOUT_FRIENDLY_URL = PropsUtil.get(PropsKeys.DEFAULT_USER_PUBLIC_LAYOUT_FRIENDLY_URL);
    public static final String DEFAULT_USER_PUBLIC_LAYOUT_NAME = PropsUtil.get(PropsKeys.DEFAULT_USER_PUBLIC_LAYOUT_NAME);
    public static final String DEFAULT_USER_PUBLIC_LAYOUT_TEMPLATE_ID = PropsUtil.get(PropsKeys.DEFAULT_USER_PUBLIC_LAYOUT_TEMPLATE_ID);
    public static final String DEFAULT_USER_PUBLIC_LAYOUT_REGULAR_THEME_ID = PropsUtil.get(PropsKeys.DEFAULT_USER_PUBLIC_LAYOUT_REGULAR_THEME_ID);
    public static final String DEFAULT_USER_PUBLIC_LAYOUT_REGULAR_COLOR_SCHEME_ID = PropsUtil.get(PropsKeys.DEFAULT_USER_PUBLIC_LAYOUT_REGULAR_COLOR_SCHEME_ID);
    public static boolean LAYOUT_USER_PRIVATE_LAYOUTS_ENABLED = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.LAYOUT_USER_PRIVATE_LAYOUTS_ENABLED));
    public static boolean LAYOUT_USER_PRIVATE_LAYOUTS_AUTO_CREATE = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.LAYOUT_USER_PRIVATE_LAYOUTS_AUTO_CREATE));
    public static boolean LAYOUT_USER_PRIVATE_LAYOUTS_POWER_USER_REQUIRED = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.LAYOUT_USER_PRIVATE_LAYOUTS_POWER_USER_REQUIRED));
    public static boolean LAYOUT_USER_PUBLIC_LAYOUTS_ENABLED = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.LAYOUT_USER_PUBLIC_LAYOUTS_ENABLED));
    public static boolean LAYOUT_USER_PUBLIC_LAYOUTS_AUTO_CREATE = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.LAYOUT_USER_PUBLIC_LAYOUTS_AUTO_CREATE));
    public static boolean LAYOUT_USER_PUBLIC_LAYOUTS_POWER_USER_REQUIRED = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.LAYOUT_USER_PUBLIC_LAYOUTS_POWER_USER_REQUIRED));
}
