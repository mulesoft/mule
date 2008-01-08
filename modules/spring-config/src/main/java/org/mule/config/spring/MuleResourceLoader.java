/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * <code>MuleResourceLoader</code> is a custom Spring resource loader that calls
 * standard Mule methods for loading resource files.
 */
public class MuleResourceLoader extends DefaultResourceLoader implements ResourcePatternResolver
{
    protected transient Log logger = LogFactory.getLog(MuleResourceLoader.class);

    public Resource getResource(String rsc)
    {
        return getResourceByPath(rsc);
    }

    protected Resource getResourceByPath(String path)
    {
        InputStream is = null;
        try
        {
            is = IOUtils.getResourceAsStream(path, getClass());
        }
        catch (IOException e)
        {
            logger.error("Unable to load Spring resource " + path + " : " + e.getMessage());
            return null;
        }

        if (is != null)
        {
            return new InputStreamResource(is);
        }
        else
        {
            logger.error("Unable to locate Spring resource " + path);
            return null;
        }
    }

    public Resource[] getResources(String rsc) throws IOException
    {
        if (rsc == null)
        {
            throw new IOException("No resources to read");
        }
        String[] resourcesNames = org.springframework.util.StringUtils.tokenizeToStringArray(rsc, ",;", true,
            true);
        Resource[] resources = new Resource[resourcesNames.length];
        for (int i = 0; i < resourcesNames.length; ++i)
        {
            resources[i] = getResourceByPath(resourcesNames[i]);
        }
        return resources;
    }
}
