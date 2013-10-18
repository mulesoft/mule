/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.extras;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.module.cxf.SoapConstants;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.soap.axis.AxisConnector;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class AxisCleanAndAddProperties
{
    
    // add all custom headers, filter out all mule headers (such as
    // MULE_SESSION) except
    // for MULE_USER header. Filter out other headers like "soapMethods" and
    // MuleProperties.MULE_METHOD_PROPERTY and "soapAction"
    // and also filter out any http related header
    
    public static Map<String, Object> cleanAndAdd(MuleEventContext muleEventContext){
        
        Map<String, Object> props = new HashMap<String, Object>();
        MuleMessage currentMessage = muleEventContext.getMessage();

        populateProps(props, currentMessage, PropertyScope.INVOCATION);
        populateProps(props, currentMessage, PropertyScope.OUTBOUND);
        return props;
    }

    protected static void populateProps(Map<String, Object> props, MuleMessage currentMessage, PropertyScope scope)
    {
        for (String name : currentMessage.getPropertyNames(scope))
        {
            if (!StringUtils.equals(name, AxisConnector.SOAP_METHODS)
                && !StringUtils.equals(name, SoapConstants.SOAP_ACTION_PROPERTY)
                && !StringUtils.equals(name, MuleProperties.MULE_METHOD_PROPERTY)
                && (!name.startsWith(MuleProperties.PROPERTY_PREFIX) || StringUtils.equals(name,
                    MuleProperties.MULE_USER_PROPERTY))
                && !HttpConstants.ALL_HEADER_NAMES.containsValue(name)
                && !StringUtils.equals(name, HttpConnector.HTTP_STATUS_PROPERTY))
            {
                props.put(name, currentMessage.getProperty(name, scope));
            }
        }
    }
}
