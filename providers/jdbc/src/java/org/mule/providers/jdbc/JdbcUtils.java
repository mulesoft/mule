/*
 * Created on 23 févr. 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mule.providers.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author gnt
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class JdbcUtils {

	private static final transient Log logger = LogFactory.getLog(JdbcUtils.class);
	
	public static void close(Connection con) throws SQLException {
		if (con != null) {
			con.close();
		}
	}
	
	public static void commitAndClose(Connection con) throws SQLException {
		if (con != null) {
			con.commit();
			con.close();
		}
	}
	
	public static void rollbackAndClose(Connection con) throws SQLException {
		if (con != null) {
			con.rollback();
			con.close();
		}
	}
	
	/**
	 * Parse the given statement filling the parameter list and return
	 * the ready to use statement.
	 * 
	 * @param stmt
	 * @param params
	 * @return
	 */
	public static String parseStatement(String stmt, List params) {
		if (stmt == null) {
			return stmt;
		}
		Pattern p = Pattern.compile("\\$\\{[^\\}]*\\}");
		Matcher m = p.matcher(stmt);
		StringBuffer sb = new StringBuffer();
	    while (m.find()) {
	    	String key = m.group();
	    	m.appendReplacement(sb, "?");
	    	params.add(key);
	    }
	    m.appendTail(sb);
	    return sb.toString();
	}
	
	public static Object[] getParams(UMOEndpointURI uri, List paramNames, Object root) throws Exception {
		Object[] params = new Object[paramNames.size()];
		for (int i = 0; i < paramNames.size(); i++) {
			String param = (String) paramNames.get(i);
			String name  = param.substring(2, param.length() - 1);
			Object value = null;
			if ("NOW".equals(name)) {
				value = new Timestamp(Calendar.getInstance().getTimeInMillis());
			} else {
				try {
					value = BeanUtils.getProperty(root, name);
				} catch (Exception ignored) {
				}
			}
			if (value == null) {
				value = uri.getParams().getProperty(name);
			}
			if (value == null) {
				throw new IllegalArgumentException("Can not retrieve argument " + name);
			}
			params[i] = value;
		}
		return params;
	}
	
}
