/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.umo.UMOException;
import org.mule.util.IOUtils;
import org.mule.util.StringMessageUtils;

/**
 * <code>MuleService</code> exposes certain Mule server functions for
 * management
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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
    private String buildDate;
    //TODO
    private String copyright = "Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com";
    private String license;

    public MuleService() {
        String patch = System.getProperty("sun.os.patch.level", null);
        jdk = System.getProperty("java.version") + " (" + System.getProperty("java.vm.info") + ")";
        os = System.getProperty("os.name");
        if(patch!=null && !"unknown".equalsIgnoreCase(patch)) {
            os += " - " + patch;
        }
        os += " (" + System.getProperty("os.version") + ", " + System.getProperty("os.arch") + ")";

        buildDate = MuleManager.getConfiguration().getBuildDate();
        try {
            InetAddress iad = InetAddress.getLocalHost();
            host = iad.getCanonicalHostName();
            ip = iad.getHostAddress();
        } catch (UnknownHostException e) {
            // ignore
        }
    }

    public boolean isInstanciated()
    {
        return MuleManager.isInstanciated();
    }

    public boolean isInitialised()
    {
        return isInstanciated() && MuleManager.getInstance().isInitialised();
    }

    public boolean isStopped()
    {
        return isInstanciated() && !MuleManager.getInstance().isStarted();
    }

    public Date getStartTime()
    {
        if (!isStopped()) {
            return new Date(MuleManager.getInstance().getStartDate());
        } else {
            return null;
        }
    }

    public String getVersion()
    {
        if (version == null) {
            version = MuleManager.getConfiguration().getProductVersion();
            if (version == null) {
                version = "Mule Version Info Not Set";
            }
        }
        return version;
    }

    public String getVendor()
    {
        if (vendor == null) {
            vendor = MuleManager.getConfiguration().getVendorName();
            if (vendor == null) {
                vendor = "Mule Vendor Info Not Set";
            }
        }
        return vendor;
    }

    public void start() throws UMOException
    {
        MuleManager.getInstance().start();
    }

    public void stop() throws UMOException
    {
        MuleManager.getInstance().stop();
    }

    public void dispose() throws UMOException
    {
        MuleManager.getInstance().dispose();
    }

    public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public String getServerId() {
        return MuleManager.getInstance().getId();
    }

    public String getHostname() {
        return host;
    }

    public String getHostIp() {
        return ip;
    }

    public String getOsVersion() {
        return os;
    }

    public String getJdkVersion() {
        return jdk;
    }

    public String getCopyright() {
        return copyright;
    }

    public String getLicense() {
        if(license==null) {
            try {
                license = IOUtils.getResourceAsString("MULE_LICENSE.txt", getClass());
                license = StringMessageUtils.getBoilerPlate(license, ' ', 80);
            } catch (IOException e) {
                logger.warn("Failed to load LICENSE.txt", e);
            }
            if(license == null) {
                license = "Failed to load license";
            }
        }
        return license;
    }

    public String getBuildDate() {
        return buildDate;
    }

    public String getInstanceId() {
        return MuleManager.getInstance().getId();
    }
}
