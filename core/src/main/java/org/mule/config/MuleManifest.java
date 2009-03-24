/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.api.config.MuleConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a static class that provides access to the Mule core manifest file. 
 */
// TODO EE-572
public class MuleManifest
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleManifest.class);

    private static Manifest manifest;

    public static String getProductVersion()
    {
        return getManifestProperty("Implementation-Version");
    }

    public static String getVendorName()
    {
        return getManifestProperty("Specification-Vendor");
    }

    public static String getVendorUrl()
    {
        return getManifestProperty("Vendor-Url");
    }

    public static String getProductUrl()
    {
        return getManifestProperty("Product-Url");
    }

    public static String getProductName()
    {
        return getManifestProperty("Implementation-Title");
    }

    public static String getProductMoreInfo()
    {
        return getManifestProperty("More-Info");
    }

    public static String getProductSupport()
    {
        return getManifestProperty("Support");
    }

    public static String getProductLicenseInfo()
    {
        return getManifestProperty("License");
    }

    public static String getProductDescription()
    {
        return getManifestProperty("Description");
    }

    public static String getBuildNumber()
    {
        return getManifestProperty("Build-Revision");
    }
    
    public static String getBuildDate()
    {
        return getManifestProperty("Build-Date");
    }

    public static String getDevListEmail()
    {
        return getManifestProperty("Dev-List-Email");
    }

    public static String getDTDSystemId()
    {
        return getManifestProperty("Dtd-System-Id");
    }

    public static String getDTDPublicId()
    {
        return getManifestProperty("Dtd-Public-Id");
    }

    public static Manifest getManifest()
    {
        if (manifest == null)
        {
            manifest = new Manifest();

            InputStream is = null;
            try
            {
                // We want to load the MANIFEST.MF from the mule-core jar. Sine we
                // don't know the version we're using we have to search for the jar on the classpath
                URL url = (URL) AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        try
                        {
                            Enumeration e = MuleConfiguration.class.getClassLoader().getResources(
                                    ("META-INF/MANIFEST.MF"));
                            while (e.hasMoreElements())
                            {
                                URL url = (URL) e.nextElement();
                                if ((url.toExternalForm().indexOf("mule-core") > -1 && url.toExternalForm()
                                    .indexOf("tests.jar") < 0)
                                    || url.toExternalForm().matches(".*mule.*-.*-embedded.*\\.jar.*"))
                                {
                                    return url;
                                }
                            }
                        }
                        catch (IOException e1)
                        {
                            logger.warn("Failure reading manifest: " + e1.getMessage(), e1);
                        }
                        return null;
                    }
                });

                if (url != null)
                {
                    is = url.openStream();
                }

                if (is != null)
                {
                    manifest.read(is);
                }
            }
            catch (IOException e)
            {
                logger.warn("Failed to read manifest Info, Manifest information will not display correctly: "
                        + e.getMessage());
            }
        }
        return manifest;
    }

    protected static String getManifestProperty(String name)
    {
        return getManifest().getMainAttributes().getValue(new Attributes.Name(name));
    }
}
