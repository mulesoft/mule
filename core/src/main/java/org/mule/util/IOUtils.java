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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// @ThreadSafe
/**
 * Mule input/output utilities.
 */
public class IOUtils extends org.apache.commons.io.IOUtils
{
    /** Logger. */
    private static final Log logger = LogFactory.getLog(IOUtils.class);

    /**
     * Attempts to load a resource from the file system, from a URL, or from the
     * classpath, in that order.
     * 
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     * @return the requested resource as a string
     * @throws java.io.IOException IO error
     */
    public static String getResourceAsString(final String resourceName, final Class callingClass)
        throws IOException
    {
        InputStream is = getResourceAsStream(resourceName, callingClass);
        if (is != null)
        {
            return toString(is);
        }
        else
        {
            throw new IOException("Unable to load resource " + resourceName);
        }
    }

    /**
     * Attempts to load a resource from the file system, from a URL, or from the
     * classpath, in that order.
     * 
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     * @return an InputStream to the resource or null if resource not found
     * @throws java.io.IOException IO error
     */
    public static InputStream getResourceAsStream(final String resourceName, final Class callingClass)
        throws IOException
    {
        return getResourceAsStream(resourceName, callingClass, true, true);
    }

    /**
     * Attempts to load a resource from the file system, from a URL, or from the
     * classpath, in that order.
     * 
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     * @param tryAsFile - try to load the resource from the local file system
     * @param tryAsUrl - try to load the resource as a URL
     * @return an InputStream to the resource or null if resource not found
     * @throws java.io.IOException IO error
     */
    public static InputStream getResourceAsStream(final String resourceName,
                                                  final Class callingClass,
                                                  boolean tryAsFile,
                                                  boolean tryAsUrl) throws IOException
    {

        URL url = getResourceAsUrl(resourceName, callingClass, tryAsFile);

        // Try to load the resource itself as a URL.
        if ((url == null) && (tryAsUrl))
        {
            try
            {
                url = new URL(resourceName);
            }
            catch (MalformedURLException e)
            {
                logger.debug("Unable to load resource as a URL: " + resourceName);
            }
        }

        if (url == null)
        {
            return null;
        }
        else
        {
            return url.openStream();
        }
    }

    /**
     * Attempts to load a resource from the file system or from the classpath, in
     * that order.
     * 
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     * @return an URL to the resource or null if resource not found
     */
    public static URL getResourceAsUrl(final String resourceName, final Class callingClass)
    {
        return getResourceAsUrl(resourceName, callingClass, true);
    }

    /**
     * Attempts to load a resource from the file system or from the classpath, in
     * that order.
     * 
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     * @param tryAsFile - try to load the resource from the local file system
     * @return an URL to the resource or null if resource not found
     */
    public static URL getResourceAsUrl(final String resourceName, final Class callingClass, boolean tryAsFile)
    {
        if (resourceName == null)
        {
            throw new IllegalArgumentException(new Message(Messages.X_IS_NULL, "Resource name").getMessage());
        }
        URL url = null;

        // Try to load the resource from the file system.
        if (tryAsFile)
        {
            try
            {
                File file = FileUtils.newFile(resourceName);
                if (file.exists())
                {
                    url = file.getAbsoluteFile().toURL();
                }
                else
                {
                    logger.debug("Unable to load resource from the file system: " + file.getAbsolutePath());
                }
            }
            catch (Exception e)
            {
                logger.debug("Unable to load resource from the file system: " + e.getMessage());
            }
        }

        // Try to load the resource from the classpath.
        if (url == null)
        {
            try
            {
                url = (URL)AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        return ClassUtils.getResource(resourceName, callingClass);
                    }
                });
                if (url == null)
                {
                    logger.debug("Unable to load resource " + resourceName + " from the classpath");
                }
            }
            catch (Exception e)
            {
                logger.debug("Unable to load resource " + resourceName + " from the classpath: " + e.getMessage());
            }
        }

        return url;
    }
}
