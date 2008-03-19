/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.config.i18n.CoreMessages;

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
    public static InputStream getResourceAsStream(final String resourceName,
                                                  final Class callingClass) throws IOException
    {
        return getResourceAsStream(parseResourceName(resourceName), callingClass, true, true);
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

        URL url = getResourceAsUrl(resourceName, callingClass, tryAsFile, tryAsUrl);

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
        return getResourceAsUrl(resourceName, callingClass, true, true);
    }

    /**
     * Attempts to load a resource from the file system or from the classpath, in
     * that order.
     * 
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     * @param tryAsFile - try to load the resource from the local file system
     * @param tryAsUrl - try to load the resource as a Url string
     * @return an URL to the resource or null if resource not found
     */
    public static URL getResourceAsUrl(final String resourceName,
                                       final Class callingClass,
                                       boolean tryAsFile, boolean tryAsUrl)
    {
        if (resourceName == null)
        {
            throw new IllegalArgumentException(
                CoreMessages.objectIsNull("Resource name").getMessage());
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
                    logger.debug("Unable to load resource from the file system: "
                                 + file.getAbsolutePath());
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

        if(url==null)
        {
            try
            {
                url = new URL(resourceName);
            }
            catch (MalformedURLException e)
            {
                //ignore
            }
        }
        return url;
    }

    /**
     * This method checks whether the name of the resource needs to be parsed. If it
     * is, it parses the name and tries to get the variable from the Environmental
     * Variables configured on the system.
     * 
     * @param src
     * @return
     */
    private static String parseResourceName(String src)
    {
        String var;
        String[] split;
        String ps = File.separator;

        if (src.indexOf('$') > -1)
        {
            split = src.split("}");
        }
        else
        {
            return src;
        }

        var = split[0].substring(2);
        var = SystemUtils.getenv(var);
        if (split.length > 1)
        {
            if (var == null)
            {
                var = System.getProperty(split[0].substring(2));
                if (var == null)
                {
                    return split[1].substring(1);
                }
                else
                {
                    return var + ps + split[1].substring(1);
                }
            }
            else
            {
                return var + ps + split[1].substring(1);
            }
        }
        else
        {
            if (var == null)
            {
                return "";
            }
            else
            {
                return var;
            }
        }
    }
    
    /**
     * This method wraps {@link org.apache.commons.io.IOUtils}' <code>toString(InputStream)</code>
     * method but catches any {@link IOException} and wraps it into a {@link RuntimeException}.
     */
    public static String toString(InputStream input)
    {
        try
        {
            return org.apache.commons.io.IOUtils.toString(input);
        }
        catch (IOException iox)
        {
            throw new RuntimeException(iox);
        }
    }
    
    /**
     * This method wraps {@link org.apache.commons.io.IOUtils}' <code>toByteArray(InputStream)</code>
     * method but catches any {@link IOException} and wraps it into a {@link RuntimeException}.
     */
    public static byte[] toByteArray(InputStream input)
    {
        try
        {
            return org.apache.commons.io.IOUtils.toByteArray(input);
        }
        catch (IOException iox)
        {
            throw new RuntimeException(iox);
        }
    }
}
