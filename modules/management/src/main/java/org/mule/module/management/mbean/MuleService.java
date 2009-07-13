/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.management.mbean;

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.config.MuleManifest;
import org.mule.util.IOUtils;
import org.mule.util.StringMessageUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleService</code> exposes certain Mule server functions for management
 */
public class MuleService implements MuleServiceMBean
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private String version;
    private String vendor;
    private String jdk;
    private String host;
    private String ip;
    private String os;
    private String buildNumber;
    private String buildDate;
    // TODO
    private String copyright = "Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com";
    private String license;

    private MuleContext muleContext;

    public MuleService(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        String patch = System.getProperty("sun.os.patch.level", null);
        jdk = System.getProperty("java.version") + " (" + System.getProperty("java.vm.info") + ")";
        os = System.getProperty("os.name");
        if (patch != null && !"unknown".equalsIgnoreCase(patch))
        {
            os += " - " + patch;
        }
        os += " (" + System.getProperty("os.version") + ", " + System.getProperty("os.arch") + ")";

        buildNumber = MuleManifest.getBuildNumber();
        buildDate = MuleManifest.getBuildDate();
        try
        {
            InetAddress iad = InetAddress.getLocalHost();
            host = iad.getCanonicalHostName();
            ip = iad.getHostAddress();
        }
        catch (UnknownHostException e)
        {
            // ignore
        }
    }

    public boolean isInitialised()
    {
        return muleContext!=null && muleContext.isInitialised();
    }

    public boolean isStopped()
    {
        return muleContext!=null && !muleContext.isStarted();
    }

    public Date getStartTime()
    {
        if (!isStopped())
        {
            return new Date(muleContext.getStartDate());
        }
        else
        {
            return null;
        }
    }

    public String getVersion()
    {
        if (version == null)
        {
            version = MuleManifest.getProductVersion();
            if (version == null)
            {
                version = "Mule Version Info Not Set";
            }
        }
        return version;
    }

    public String getVendor()
    {
        if (vendor == null)
        {
            vendor = MuleManifest.getVendorName();
            if (vendor == null)
            {
                vendor = "Mule Vendor Info Not Set";
            }
        }
        return vendor;
    }

    public void start() throws MuleException
    {
        muleContext.start();
    }

    public void stop() throws MuleException
    {
        muleContext.stop();
    }

    public void dispose() throws MuleException
    {
        muleContext.dispose();
    }

    public long getFreeMemory()
    {
        return Runtime.getRuntime().freeMemory();
    }

    public long getMaxMemory()
    {
        return Runtime.getRuntime().maxMemory();
    }

    public long getTotalMemory()
    {
        return Runtime.getRuntime().totalMemory();
    }

    public String getServerId()
    {
        return muleContext.getConfiguration().getId();
    }

    public String getHostname()
    {
        return host;
    }

    public String getHostIp()
    {
        return ip;
    }

    public String getOsVersion()
    {
        return os;
    }

    public String getJdkVersion()
    {
        return jdk;
    }

    public String getCopyright()
    {
        return copyright;
    }

    public String getLicense()
    {
        if (license == null)
        {
            loadEnterpriseLicense();
            if (license == null)
            {
                loadCommunityLicense();
            }
            
            if (license == null)
            {
                license = "Failed to load license";
            }
        }
        return license;
    }
    
    private void loadEnterpriseLicense()
    {        
        try
        {
            loadLicense("MULE_EE_LICENSE.txt");
        }
        catch (IOException e)
        {
            // this will happen if running in a CE distribution and is not an error per se
        }        
    }
    
    private void loadCommunityLicense()
    {
        try
        {
            loadLicense("MULE_LICENSE.txt");
        }
        catch (IOException e)
        {
            logger.warn("Failed to load MULE_LICENSE.txt", e);
        }        
    }

    private void loadLicense(String licenseFile) throws IOException
    {
        license = IOUtils.getResourceAsString(licenseFile, getClass());
        license = StringMessageUtils.getBoilerPlate(license, ' ', 80);
    }
    
    /**
     * @deprecated use getBuildNumber() instead
     */
    public String getBuildDate()
    {
        return buildDate;
    }

    public String getBuildNumber()
    {
        return buildNumber;
    }

    public String getInstanceId()
    {
        return muleContext.getConfiguration().getId();
    }

    public String getConfigBuilderClassName()
    {
        return MuleServer.getConfigBuilderClassName();
    }
}
