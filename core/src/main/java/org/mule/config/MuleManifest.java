/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import java.util.SortedMap;
import java.util.TreeMap;
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
        final String version = getManifestProperty("Implementation-Version");
        return version == null ? "Unknown" : version;
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

	public static String getSupportedJdks()
	{
		return getManifestProperty("Supported-Jdks");
	}

	public static String getRecommndedJdks()
	{
		return getManifestProperty("Recommended-Jdks");
	}

    // synchronize this method as manifest initialized here.
    public static synchronized Manifest getManifest()
    {
        if (manifest == null)
        {
            manifest = new Manifest();


            InputStream is = null;
            try
            {
                // We want to load the MANIFEST.MF from the mule-core jar. Sine we
                // don't know the version we're using we have to search for the jar on the classpath
                URL url = AccessController.doPrivileged(new UrlPrivilegedAction());

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

    /**
     * To be used only for testing, to provide a custom manifest
     */
    public static synchronized void initializeManifest(URL manifestUrl)
    {
        manifest = new Manifest();

        try(InputStream is = manifestUrl.openStream())
        {
            manifest.read(is);
        }
        catch (IOException e)
        {
            logger.warn("Failed to read manifest Info, Manifest information will not display correctly: "
                        + e.getMessage());
        }

    }

    protected static String getManifestProperty(String name)
    {
        return getManifest().getMainAttributes().getValue(new Attributes.Name(name));
    }

    static class UrlPrivilegedAction implements PrivilegedAction<URL>
    {
        public URL run()
        {
            URL result = null;
            try
            {
                Enumeration<URL> e = MuleConfiguration.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
                result = getManifestJarURL(e);
                if (result == null)
                {
                    // if we haven't found a valid manifest yet, maybe we're running tests
                    result = getManifestTestJarURL();
                }
            }
            catch (IOException e1)
            {
                logger.warn("Failure reading manifest: " + e1.getMessage(), e1);
            }
            return result;
        }

        URL getManifestJarURL(Enumeration<URL> e)
        {
            SortedMap<String, URL> candidates = new TreeMap<String, URL>();
            while (e.hasMoreElements())
            {
                URL url = e.nextElement();
                if ((url.toExternalForm().indexOf("mule-core") > -1 && url.toExternalForm().indexOf("tests.jar") < 0)
                    || url.toExternalForm().matches(".*mule.*-.*-embedded.*\\.jar.*"))
                {
                    candidates.put(url.toExternalForm(), url);
                }
            }
            if (!candidates.isEmpty())
            {
                //if mule-core and mule-core-ee jars are present, then mule-core-ee gets precedence
                return candidates.get(candidates.lastKey());
            }
            return null;
        }

        URL getManifestTestJarURL() throws IOException
        {
            String pathSeparator = System.getProperty("file.separator");
            String testManifestPath = "core" + pathSeparator + "target" + pathSeparator + "test-classes";
            String testManifestPath2 = "core" + pathSeparator + "build" + pathSeparator + "resources" + pathSeparator + "test";
            Enumeration<URL> e = MuleConfiguration.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (e.hasMoreElements())
            {
                URL url = e.nextElement();
                if (((url.toExternalForm().contains(testManifestPath) || url.toExternalForm().contains(testManifestPath2))
                        && !url.toExternalForm().contains("tests.jar")))
                {
                    return url;
                }
            }
            return null;
        }
    }
}
