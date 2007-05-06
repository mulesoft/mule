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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This is a convenience Entity Resolver that will resolve all entities defined in
 * the META-INF directory from the class path. Spring defaults to doing this from File since
 * the 'systemId' is prepended with file:/
 *
 * By using this resolver, users only have to reference schameLocations relative to the META-INF classpath.  This is
 * commonly used when defining custom namespases for a Mule module or transport.
 */
public class MuleDelegatingClasspathEntityResolver extends ResourceEntityResolver
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(MuleDelegatingClasspathEntityResolver.class);

    public static final String BASE_RESOURCE_PATH = "/META-INF";

    private ResourceLoader resourceLoader;
    public MuleDelegatingClasspathEntityResolver(ResourceLoader resourceLoader)
    {
        super(resourceLoader);
        this.resourceLoader = resourceLoader;
    }

    //@Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
    {
        if(systemId.indexOf(BASE_RESOURCE_PATH) > 0)
        {
            String resourcePath = systemId.substring(systemId.lastIndexOf(BASE_RESOURCE_PATH));

            Resource resource = this.resourceLoader.getResource(resourcePath);
            if(resource!=null)
            {
                InputSource source = new InputSource(resource.getInputStream());
                source.setPublicId(publicId);
                source.setSystemId(systemId);
                if (logger.isDebugEnabled()) {
                    logger.debug("Found XML entity [" + systemId + "]: " + resource);
                }
                return source;
            }
        }
        return super.resolveEntity(publicId, systemId);
    }
}

