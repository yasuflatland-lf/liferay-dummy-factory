package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CommonUtil.class)
public class CommonUtil {


	/**
	 * Page Command Pairs
	 * 
	 * @return jsp file name corresponding to the command.
	 */
	public Map<String,String> getPageFromMode() {
		return LDFPortletKeys.renderJSPs;
	}

	/**
	 * Convert string array to long array
	 * 
	 * @param source String array of ids
	 * @return long array of ids
	 */
	public long[] convertStringToLongArray(String[] source) {
		if(Validator.isNull(source) || source.length <= 0) {
			return null;
		}
		
		return Arrays.stream(source).mapToLong(Long::parseLong).toArray();
	}	

	/**
	 * Filter roles
	 * 
	 * Depending on role types, the processes to apply roles are different. This
	 * method filter either Organization / Site roles or Regular roles.
	 * 
	 * @param roleIds role ids. This can be mix of reguler / organization / site roles.
	 * @return Filtered roleids in each array.
	 * @throws PortalException
	 */
	public Map<Integer, List<Role>> filterRoles(long[] roleIds) throws PortalException {
		
		if(Validator.isNull(roleIds) || roleIds.length == 0) {
			return new ConcurrentHashMap<Integer, List<Role>>();
		}
		
		List<Role> roles = _roleLocalService.getRoles(roleIds);
        return roles.stream().collect(
    	    Collectors.groupingBy(Role::getType)
    	);        
	}	
	
	@Reference
	private RoleLocalService _roleLocalService;	
}
