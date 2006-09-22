/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import org.apache.commons.beanutils.PropertyUtils;
import org.dom4j.Node;
import org.dom4j.io.DOMReader;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public abstract class JdbcUtils
{

    public static void close(Connection con) throws SQLException
    {
        if (con != null && !con.isClosed())
        {
            con.close();
        }
    }

    public static void commitAndClose(Connection con) throws SQLException
    {
        if (con != null)
        {
            if (con.getAutoCommit() == false)
            {
                con.commit();
            }
            con.close();
        }
    }

    public static void rollbackAndClose(Connection con) throws SQLException
    {
        if (con != null)
        {
            if (con.getAutoCommit() == false)
            {
                con.rollback();
            }
            con.close();
        }
    }

    /**
     * Parse the given statement filling the parameter list and return the ready to
     * use statement.
     * 
     * @param stmt
     * @param params
     * @return
     */
    public static String parseStatement(String stmt, List params)
    {
        if (stmt == null)
        {
            return stmt;
        }
        Pattern p = Pattern.compile("\\$\\{[^\\}]*\\}");
        Matcher m = p.matcher(stmt);
        StringBuffer sb = new StringBuffer(200);
        while (m.find())
        {
            String key = m.group();
            m.appendReplacement(sb, "?");
            params.add(key);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Object[] getParams(UMOImmutableEndpoint endpoint, List paramNames, Object root)
            throws Exception
    {
        Object[] params = new Object[paramNames.size()];
        for (int i = 0; i < paramNames.size(); i++)
        {
            String param = (String)paramNames.get(i);
            String name = param.substring(2, param.length() - 1);
            Object value = null;
            // If we find a value and it happens to be null, thats acceptable
            boolean foundValue = false;

            if ("NOW".equalsIgnoreCase(name))
            {
                value = new Timestamp(Calendar.getInstance().getTimeInMillis());
                foundValue = true;
            }
            // TODO Document what this is all about.  What does parsing XML have to do with JDBC??
            else if (root instanceof org.w3c.dom.Document)
            {
                org.w3c.dom.Document x3cDoc = (org.w3c.dom.Document)root;
                org.dom4j.Document dom4jDoc = new DOMReader().read(x3cDoc);
                try
                {
                    Node node = dom4jDoc.selectSingleNode(name);
                    if (node != null)
                    {
                        value = node.getText();
                        foundValue = true;
                    }
                }
                catch (Exception ignored)
                {
                    // ignore
                }
            }
            else if (root instanceof org.dom4j.Document)
            {
                org.dom4j.Document dom4jDoc = (org.dom4j.Document)root;
                try
                {
                    Node node = dom4jDoc.selectSingleNode(name);
                    if (node != null)
                    {
                        value = node.getText();
                        foundValue = true;
                    }
                }
                catch (Exception ignored)
                {
                    // ignore
                }
            }
            else if (root instanceof org.dom4j.Node)
            {
                org.dom4j.Node dom4jNode = (org.dom4j.Node)root;
                try
                {
                    Node node = dom4jNode.selectSingleNode(name);
                    if (node != null)
                    {
                        value = node.getText();
                        foundValue = true;
                    }
                }
                catch (Exception ignored)
                {
                    // ignore
                }
            }
            else
            {
                try
                {
                    value = PropertyUtils.getProperty(root, name);
                    foundValue = (value != null);
                }
                catch (Exception ignored)
                {
                    // ignore
                }
            }
            if (value == null)
            {
                value = endpoint.getProperty(name);
                foundValue = foundValue || endpoint.getProperties().containsKey(name);
            }
            if (name.equals("payload"))
            {
                value = root;
                foundValue = true;
            }
            // Allow null values which may be acceptable to the user
            if (value == null && !foundValue)
            {
                throw new IllegalArgumentException("Can not retrieve argument " + name);
            }
            params[i] = value;
        }

        return params;
    }

}
