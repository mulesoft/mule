/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

public class IOUtils extends org.apache.commons.io.IOUtils
{
    protected static Log logger = LogFactory.getLog(IOUtils.class);

    /**
     * Attempts to load a resource from the file system, from a URL, or from the
     * classpath, in that order.
     *
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     * @param tryAsFile - try to load the resource from the local file system
     * @param tryAsUrl - try to load the resource as a URL
     * @return an InputStream to the resource or null if resource not found
     */
    public static InputStream getResourceAsStream(final String resourceName,
            final Class callingClass, boolean tryAsFile, boolean tryAsUrl) {
        if (resourceName == null) {
            throw new IllegalArgumentException(new Message(Messages.X_IS_NULL, "Resource name").getMessage());
        }
        InputStream is = null;

        // Try to load the resource from the file system.
        if (tryAsFile) {
            try {
                File file = new File(resourceName);
                if (file.exists()) {
                    is = new FileInputStream(file);
                } else {
                    logger.debug("Unable to load resource from the file system: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                logger.debug("Unable to load resource from the file system: " + e.getMessage());
            }
        }

        // Try to load the resource as a URL.
        if ((is == null) && tryAsUrl) {
            try {
                URL url = new URL(resourceName);
                is = url.openStream();
                if (is == null) {
                    logger.debug("Unable to load resource as a URL");
                }
            } catch (Exception e) {
                logger.debug("Unable to load resource as a URL: " + e.getMessage());
            }
        }

        // Try to load the resource from the classpath.
        if (is == null) {
            try {
                is = (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        URL url = ClassUtils.getResource(resourceName, callingClass);
                        try {
                            return (url != null) ? url.openStream() : null;
                        } catch (IOException e) {
                            return null;
                        }
                    }
                });
                if (is == null) {
                    logger.debug("Unable to load resource from the classpath");
                }
            } catch (Exception e) {
                logger.debug("Unable to load resource from the classpath: " + e.getMessage());
            }
        }

        return is;
    }
}
