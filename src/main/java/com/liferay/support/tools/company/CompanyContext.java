package com.liferay.support.tools.company;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.ParamContext;

import javax.portlet.ActionRequest;

public class CompanyContext extends ParamContext {

	private long numberOfCompanies;
	private String	webId;
	private String	virtualHostname;
	private String	mx;
	private boolean	system;
	private int		maxUsers;
	private boolean	active;

	public CompanyContext(ActionRequest actionRequest) {
		// Fetch data
		numberOfCompanies = ParamUtil.getLong(actionRequest, "numberOfCompanies",0);
		webId = ParamUtil.getString( actionRequest, "webId", "" );
		virtualHostname = ParamUtil
				.getString( actionRequest, "virtualHostname", "" );
		mx = ParamUtil.getString( actionRequest, "mx", "" );
		system = ParamUtil.getBoolean( actionRequest, "system", false );
		maxUsers = ParamUtil.getInteger( actionRequest, "maxUsers", 0 );
		active = ParamUtil.getBoolean( actionRequest, "active", true );
	}

	public long getNumberOfCompanies() {
		return numberOfCompanies;
	}

	public void setNumberOfCompanies( long numberOfCompanies ) {
		this.numberOfCompanies = numberOfCompanies;
	}
	
	public String getWebId() {
		return webId;
	}

	public void setWebId( String webId ) {
		this.webId = webId;
	}

	public String getVirtualHostname() {
		return virtualHostname;
	}

	public void setVirtualHostname( String virtualHostname ) {
		this.virtualHostname = virtualHostname;
	}

	public String getMx() {
		return mx;
	}

	public void setMx( String mx ) {
		this.mx = mx;
	}

	public boolean isSystem() {
		return system;
	}

	public void setSystem( boolean system ) {
		this.system = system;
	}

	public int getMaxUsers() {
		return maxUsers;
	}

	public void setMaxUsers( int maxUsers ) {
		this.maxUsers = maxUsers;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive( boolean active ) {
		this.active = active;
	}

}