/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.registry.ServiceException;
import org.mule.api.registry.ServiceFinder;
import org.mule.transport.soap.axis.i18n.AxisMessages;
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
    @Deprecated
    public String findService(String service, ServiceDescriptor descriptor, Properties props) throws ServiceException
    {
        Map finders = new TreeMap();
        PropertiesUtils.getPropertiesWithPrefix(props, "finder.class", finders);

        StringBuilder buf = new StringBuilder();
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
        throw new ServiceException(AxisMessages.couldNotFindSoapProvider(buf.toString()));
    }

    protected String getProtocolFromKey(String key)
    {
        return key.substring(key.lastIndexOf('.') + 1);
    }
}
