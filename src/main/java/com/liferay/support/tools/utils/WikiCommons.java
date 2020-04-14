package com.liferay.support.tools.utils;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowThreadLocal;
import com.liferay.wiki.configuration.WikiGroupServiceConfiguration;
import com.liferay.wiki.engine.WikiEngine;
import com.liferay.wiki.engine.WikiEngineRenderer;
import com.liferay.wiki.model.WikiNode;
import com.liferay.wiki.model.WikiPage;
import com.liferay.wiki.model.WikiPageConstants;
import com.liferay.wiki.service.WikiNodeLocalService;
import com.liferay.wiki.service.WikiNodeLocalServiceUtil;
import com.liferay.wiki.service.WikiPageLocalService;
import com.liferay.wiki.service.WikiPageLocalServiceUtil;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.portlet.PortletRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Utilities for Wiki
 *
 * @author Yasuyuki Takeo
 */
@Component(immediate = true, service = WikiCommons.class)
public class WikiCommons {

  private static final Log _log = LogFactoryUtil.getLog(WikiCommons.class);

  private WikiEngineRenderer _wikiEngineRenderer;
  private WikiGroupServiceConfiguration _wikiGroupServiceConfiguration;
  private WikiNodeLocalService _wikiNodeLocalService;
  private WikiPageLocalService _wikiPageLocalService;

  @Reference(unbind = "-")
  protected void setWikiEngineRenderer(
      WikiEngineRenderer wikiEngineRenderer) {

    _wikiEngineRenderer = wikiEngineRenderer;
  }

  @Reference(unbind = "-")
  protected void setWikiGroupServiceConfiguration(
      WikiGroupServiceConfiguration wikiGroupServiceConfiguration) {

    _wikiGroupServiceConfiguration = wikiGroupServiceConfiguration;
  }

  @Reference(unbind = "-")
  protected void setWikiNodeLocalService(WikiNodeLocalService wikiNodeLocalService) {
    _wikiNodeLocalService = wikiNodeLocalService;
  }

  @Reference(unbind = "-")
  protected void setWikiPageLocalService(WikiPageLocalService wikiPageLocalService) {
    _wikiPageLocalService = wikiPageLocalService;
  }

  /**
   * Get Wiki format list
   *
   * @param locale
   * @return available Wiki format list
   */
  public Map<String, String> getFormats(Locale locale) {
    Collection<String> formats = _wikiEngineRenderer.getFormats();
    Map<String, String> fmt = new LinkedHashMap<>();

    for (String format : formats) {
      fmt.put(format, getFormatLabel(_wikiEngineRenderer, format, locale));
    }

    return fmt;
  }

  public String getFormatLabel(
      WikiEngineRenderer wikiEngineRenderer, String format, Locale locale) {

    WikiEngine wikiEngine = wikiEngineRenderer.fetchWikiEngine(format);

    if (wikiEngine != null) {
      return wikiEngine.getFormatLabel(locale);
    }

    return StringPool.BLANK;
  }

  /**
   * Initialize Wiki
   * <p>
   * When the first time access the wiki on the vanilla bundle, Wiki page gets exception due to no
   * data created. This method simulate the initial access to the Wiki.
   *
   * @param portletRequest
   */
  public void initWiki(PortletRequest portletRequest, long scopeGroupId) {
    try {
      if(!isNodeExist(scopeGroupId)) {
        return;
      }

      WikiNode node = getFirstVisibleNode(portletRequest, scopeGroupId);
      long nodeId = (node == null) ? 0 : node.getNodeId();
      getFirstVisiblePage(nodeId, portletRequest);

    } catch (PortalException e) {
      _log.error(e.getMessage());
    }
  }

  /**
   * Initialize Page
   *
   * @param nodeId
   * @param portletRequest
   * @throws PortalException
   */
  public void getFirstVisiblePage(
      long nodeId, PortletRequest portletRequest)
      throws PortalException {

    ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(
        WebKeys.THEME_DISPLAY);

    WikiPage page = _wikiPageLocalService.fetchPage(
        nodeId, _wikiGroupServiceConfiguration.frontPageName(), 0);

    if (page != null) {
      return;
    }

    ServiceContext serviceContext = ServiceContextFactory.getInstance(
        WikiPage.class.getName(), portletRequest);

    serviceContext.setAddGuestPermissions(true);
    serviceContext.setAddGroupPermissions(true);

    boolean workflowEnabled = WorkflowThreadLocal.isEnabled();

    try {
      WorkflowThreadLocal.setEnabled(false);

      WikiPageLocalServiceUtil.addPage(
          themeDisplay.getDefaultUserId(), nodeId,
          _wikiGroupServiceConfiguration.frontPageName(), null,
          WikiPageConstants.NEW, true, serviceContext);
    } catch (Exception e) {
      _log.error(e.getMessage());
    } finally {
      WorkflowThreadLocal.setEnabled(workflowEnabled);
    }
  }

  /**
   * Validate a Node exist
   *
   * @param scopeGroupId
   * @return true if a node exist or false
   */
  public boolean isNodeExist(long scopeGroupId) {
    int nodesCount = _wikiNodeLocalService.getNodesCount(
        scopeGroupId);

    if (nodesCount != 0) {
      _log.info("Wiki Node has already been initialized.");
      return false;
    }

    return true;
  }

  /**
   * Initialize Node
   *
   * @param portletRequest
   * @param scopeGroupId
   * @return
   * @throws PortalException
   */
  public WikiNode getFirstVisibleNode(PortletRequest portletRequest, long scopeGroupId)
      throws PortalException {
    ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(
        WebKeys.THEME_DISPLAY);
    WikiNode node = null;

    Layout layout = themeDisplay.getLayout();

    ServiceContext serviceContext = ServiceContextFactory.getInstance(
        WikiNode.class.getName(), portletRequest);

    serviceContext.setAddGroupPermissions(true);

    if (layout.isPublicLayout() || layout.isTypeControlPanel()) {
      serviceContext.setAddGuestPermissions(true);
    } else {
      serviceContext.setAddGuestPermissions(false);
    }

    serviceContext.setScopeGroupId(scopeGroupId);
    node = _wikiNodeLocalService.addDefaultNode(
        themeDisplay.getDefaultUserId(), serviceContext);

    _log.info("Wiki Node has been initialized");

    return node;
  }

}
