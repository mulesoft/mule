/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.extras;

import org.mule.config.MuleProperties;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.soap.SoapConstants;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AxisCleanAndAddProperties
{
    
    // add all custom headers, filter out all mule headers (such as
    // MULE_SESSION) except
    // for MULE_USER header. Filter out other headers like "soapMethods" and
    // MuleProperties.MULE_METHOD_PROPERTY and "soapAction"
    // and also filter out any http related header
    
    public static Map cleanAndAdd(UMOEventContext muleEventContext){
        
        Map props = new HashMap();
        UMOMessage currentMessage = muleEventContext.getMessage();
        final String SOAP_METHODS = "soapMethods";

        for (Iterator iterator = currentMessage.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String name = (String)iterator.next();
            if (!StringUtils.equals(name, SOAP_METHODS)
                && !StringUtils.equals(name, SoapConstants.SOAP_ACTION_PROPERTY)
                && !StringUtils.equals(name, MuleProperties.MULE_METHOD_PROPERTY)
                && (!name.startsWith(MuleProperties.PROPERTY_PREFIX) || StringUtils.equals(name,
                    MuleProperties.MULE_USER_PROPERTY))
                && !HttpConstants.ALL_HEADER_NAMES.containsValue(name)
                && !StringUtils.equals(name, HttpConnector.HTTP_STATUS_PROPERTY))
            {
                props.put(name, currentMessage.getProperty(name));
            }
        }
        return props;
    }
}