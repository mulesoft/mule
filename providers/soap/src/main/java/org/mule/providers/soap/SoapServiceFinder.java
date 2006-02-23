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
package org.mule.providers.soap;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.service.ConnectorFactory;
import org.mule.providers.service.ConnectorFactoryException;
import org.mule.providers.service.ConnectorServiceDescriptor;
import org.mule.providers.service.ConnectorServiceException;
import org.mule.providers.service.ConnectorServiceFinder;
import org.mule.util.ClassHelper;
import org.mule.util.PropertiesHelper;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * <code>SoapServiceFinder</code> finds a the connector service to use by
 * checking the classpath for jars required for each of the soap connector
 * implementations
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SoapServiceFinder implements ConnectorServiceFinder
{
    public ConnectorServiceDescriptor findService(String service, ConnectorServiceDescriptor csd) throws ConnectorFactoryException
    {
        Map finders = new TreeMap();
        PropertiesHelper.getPropertiesWithPrefix(csd.getProperties(), "finder.class", finders);

        StringBuffer buf = new StringBuffer();
        for (Iterator iterator = finders.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            try {
                ClassHelper.loadClass(entry.getValue().toString(), getClass());
                String protocol = getProtocolFromKey(entry.getKey().toString());
                return ConnectorFactory.getServiceDescriptor(protocol);
            } catch (ClassNotFoundException e1)
            {
                buf.append(entry.getValue().toString()).append("(").append(entry.getKey().toString()).append(")").append(", ");
            }
        }
       throw new ConnectorServiceException(new Message(Messages.COULD_NOT_FIND_SOAP_PROVIDER_X, buf.toString()));
    }

    protected String getProtocolFromKey(String key) {
        return key.substring(key.lastIndexOf('.') + 1);
    }
}
