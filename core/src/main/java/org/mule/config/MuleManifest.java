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
public class MuleManifest
{
    public static final String IMPLEMENTATION_VERSION_PROPERTY = "Implementation-Version";
    public static final String SPECIFICATION_VENDOR_PROPERTY = "Specification-Vendor";
    public static final String VENDOR_URL_PROPERTY = "Vendor-Url";
    public static final String PRODUCT_URL_PROPERTY = "Product-Url";
    public static final String IMPLEMENTATION_TITLE_PROPERTY = "Implmentation-Title";
    public static final String MORE_INFO_PROPERTY = "More-Info";
    public static final String SUPPORT_PROPERTY = "Support";
    public static final String LICENSE_PROPERTY = "License";
    public static final String DESCRIPTION_PROPERTY = "Description";
    public static final String BUILD_REVISION_PROPERTY = "Build-Revision";
    public static final String DEV_LIST_EMAIL_PROPERTY = "Dev-List-Email";
    public static final String DTD_PUBLIC_ID_PROPERTY = "Dtd-Public-Id";
    public static final String DTD_SYSTEM_ID_PROPERTY = "Dtd-System-Id";
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleManifest.class);

    private static Manifest manifest;

    public static String getProductVersion()
    {
        return getManifestProperty(IMPLEMENTATION_VERSION_PROPERTY);
    }

    public static String getVendorName()
    {
        return getManifestProperty(SPECIFICATION_VENDOR_PROPERTY);
    }

    public static String getVendorUrl()
    {
        return getManifestProperty(VENDOR_URL_PROPERTY);
    }

    public static String getProductUrl()
    {
        return getManifestProperty(PRODUCT_URL_PROPERTY);
    }

    public static String getProductName()
    {
        return getManifestProperty(IMPLEMENTATION_TITLE_PROPERTY);
    }

    public static String getProductMoreInfo()
    {
        return getManifestProperty(MORE_INFO_PROPERTY);
    }

    public static String getProductSupport()
    {
        return getManifestProperty(SUPPORT_PROPERTY);
    }

    public static String getProductLicenseInfo()
    {
        return getManifestProperty(LICENSE_PROPERTY);
    }

    public static String getProductDescription()
    {
        return getManifestProperty(DESCRIPTION_PROPERTY);
    }

    public static String getBuildNumber()
    {
        return getManifestProperty(BUILD_REVISION_PROPERTY);
    }

    public static String getDevListEmail()
    {
        return getManifestProperty(DEV_LIST_EMAIL_PROPERTY);
    }

    public static String getDTDSystemId()
    {
        return getManifestProperty(DTD_SYSTEM_ID_PROPERTY);
    }

    public static String getDTDPublicId()
    {
        return getManifestProperty(DTD_PUBLIC_ID_PROPERTY);
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
                // don't the version we're using
                // we have to search for the jar on the classpath
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
                                if (url.toExternalForm().indexOf("mule-core") > -1)
                                {
                                    return url;
                                }
                            }
                        }
                        catch (IOException e1)
                        {
                            // TODO MULE-863: Is this sufficient (was printStackTrace) and correct?
                            logger.debug("Failure reading manifest: " + e1.getMessage(), e1);
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
                // TODO MULE-863
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
