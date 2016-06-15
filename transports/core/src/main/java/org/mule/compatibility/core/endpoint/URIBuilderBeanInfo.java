/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint;

import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Required to resolve the overload of URIBuilder.setPort() during bean assembly
 */
public class URIBuilderBeanInfo extends SimpleBeanInfo
{
    protected transient final Logger logger = LoggerFactory.getLogger(URIBuilderBeanInfo.class);

    @Override
    public PropertyDescriptor[] getPropertyDescriptors()
    {
        try
        {
            Method setPort = URIBuilder.class.getMethod("setPort", new Class<?>[] {String.class});
            return new PropertyDescriptor[]
                {
                    new PropertyDescriptor("address", URIBuilder.class, null, "setAddress"),
                    new PropertyDescriptor("annotations", URIBuilder.class, null, "setAnnotations"),
                    new PropertyDescriptor("host", URIBuilder.class, null, "setHost"),
                    new PropertyDescriptor("meta", URIBuilder.class, null, "setMeta"),
                    new PropertyDescriptor("password", URIBuilder.class, null, "setPassword"),
                    new PropertyDescriptor("path", URIBuilder.class, null, "setPath"),
                    new PropertyDescriptor("port", null, setPort),
                    new PropertyDescriptor("protocol", URIBuilder.class, null, "setProtocol"),
                    new PropertyDescriptor("queryMap", URIBuilder.class, null, "setQueryMap"),
                    new PropertyDescriptor("user", URIBuilder.class, null, "setUser"),
                };
        }
        catch (Exception e)
        {
            logger.error("Error in bean introspection for URIBuilder", e);
        }
        return null;
    }
}