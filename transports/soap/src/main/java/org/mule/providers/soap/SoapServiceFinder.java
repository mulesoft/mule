/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceException;
import org.mule.registry.ServiceFinder;
import org.mule.util.ClassUtils;
import org.mule.util.PropertiesUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * <code>SoapServiceFinder</code> finds a the connector service to use by checking
 * the classpath for jars required for each of the soap connector implementations
 */
public class SoapServiceFinder implements ServiceFinder
{
    /**
     * @deprecated We can use a more intelligent strategy for locating the service using the OSGi registry.
     */
    // //@Override
    public String findService(String service, ServiceDescriptor descriptor, Properties props) throws ServiceException
    {
        Map finders = new TreeMap();
        PropertiesUtils.getPropertiesWithPrefix(props, "finder.class", finders);

        StringBuffer buf = new StringBuffer();
        for (Iterator iterator = finders.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry)iterator.next();
            try
            {
                ClassUtils.loadClass(entry.getValue().toString(), getClass());
                return getProtocolFromKey(entry.getKey().toString());
            }
            catch (ClassNotFoundException e1)
            {
                buf.append(entry.getValue().toString()).append("(").append(entry.getKey().toString()).append(
                    ")").append(", ");
            }
        }
        throw new ServiceException(new Message(Messages.COULD_NOT_FIND_SOAP_PROVIDER_X, buf.toString()));
    }

    protected String getProtocolFromKey(String key)
    {
        return key.substring(key.lastIndexOf('.') + 1);
    }
}
