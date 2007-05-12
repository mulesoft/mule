/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.xml.sax.InputSource;

/**
 * This is a convenience Entity Resolver that will resolve all entities defined in
 * the META-INF directory from the class path. Spring defaults to doing this from File since
 * the 'systemId' is prepended with file:/
 *
 * By using this resolver, users only have to reference schameLocations relative to the META-INF classpath.  This is
 * commonly used when defining custom namespases for a Mule module or transport.
 */
public class MuleDelegatingClasspathEntityResolver extends PluggableSchemaResolver
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(MuleDelegatingClasspathEntityResolver.class);

    public static final String BASE_RESOURCE_PATH = "META-INF";

    private ClassLoader resourceLoader;
    public MuleDelegatingClasspathEntityResolver(ClassLoader resourceLoader)
    {
        super(resourceLoader);
        this.resourceLoader = resourceLoader;
    }

    //@Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException
    {
        String resourcePath = systemId;
        if(systemId.indexOf(BASE_RESOURCE_PATH) > 0)
        {
            resourcePath = systemId.substring(systemId.lastIndexOf(BASE_RESOURCE_PATH));
        }
        else
        {
            resourcePath = BASE_RESOURCE_PATH + resourcePath.substring(resourcePath.lastIndexOf("/"));
        }

        URL resource = this.resourceLoader.getResource(resourcePath);
        if(resource!=null)
        {
            InputSource source = new InputSource(resource.openStream());
            source.setPublicId(publicId);
            source.setSystemId(resourcePath);
            if (logger.isDebugEnabled()) {
                logger.debug("Found XML entity [" + systemId + "]: " + resource);
            }
            return source;
        }
        return null; //super.resolveEntity(publicId, systemId);
    }
}

