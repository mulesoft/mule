/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @deprecated Mule 2.x will use the OSGi Service Registry for locating services
 */
// @ThreadSafe
public class SpiUtils
{
    private static final Log logger = LogFactory.getLog(SpiUtils.class);

    public static final String SERVICE_ROOT = "META-INF/services/";

    public static InputStream findServiceDescriptor(String path, String name, Class currentClass)
    {
        name += ".properties";
        
        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        if (!path.endsWith("/"))
        {
            path += "/";
        }
        if (path.startsWith(SERVICE_ROOT))
        {
            path += name;
        }
        else
        {
            path = SERVICE_ROOT + path + name;
        }
        try
        {
            return IOUtils.getResourceAsStream(path, currentClass, false, false);
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
