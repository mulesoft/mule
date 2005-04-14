/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.filters.xml;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.mule.umo.UMOFilter;
/**
 * <code>JXPathFilter</code> evaluates an XPath expression against an Xml document or
 * bean and returns true if the result is expected.
 *
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stéphane</a>
 * @version $Revision$
 */
public class JXPathFilter implements UMOFilter {

	private static final Logger log = Logger.getLogger(JXPathFilter.class);

	private String expression;

	private String value;

	public boolean accept(Object obj) {
		if (obj == null)
			return false;

		if (expression == null) {
			log.warn("Expression for JXPathFilter is not set");
			return false;
		}

		if (value == null) {
			log.debug("Value for JXPathFilter is not set : true by default");
			value = Boolean.TRUE.toString();
		}

		boolean res = false;

		try {
		    Object o = null;

		    if(obj instanceof String)
		     {
		         Document doc=DocumentHelper.parseText((String)obj);
		         o =doc.valueOf(expression);
		     }
		     else
		     {
		     JXPathContext context = JXPathContext.newContext(obj);
		     o =context.getValue(expression);
		     }

			log.debug("JXPathFilter Expression result='" + o
					+ "' -  Expected value='" + value + "'");

		   if(o!=null)
			res = value.equals(o.toString());
           else
           {
           	res=false;
    		log.warn("JXPathFilter Expression result is null (" + expression
					+ ")");

           }
		} catch (Exception e) {
			log.warn("JXPathFilter cannot evaluate expression (" + expression
					+ ") :" + e.getMessage(), e);
		}

		log.debug("JXPathFilter accept object  : " + res);
		return res;

	}

	/**
	 * @return XPath expression
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * @param expression
	 *            The XPath expression
	 */
	public void setExpression(String expression) {
		this.expression = expression;
	}

	/**
	 * @return The expected result value of the XPath expression
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 *            The expected result value of the XPath expression
	 */
	public void setValue(String value) {
		this.value = value;
	}
}

