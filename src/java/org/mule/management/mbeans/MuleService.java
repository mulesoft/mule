/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.mbeans;

import org.mule.MuleManager;
import org.mule.umo.UMOException;

import java.util.Date;

/**
 * <code>MuleService</code> exposes certain Mule server functions
 * for management
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleService implements MuleServiceMBean
{
    private String version;
    private String vendor;

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
        if(!isStopped()) {
        return new Date(MuleManager.getInstance().getStartDate());
        } else {
            return null;
        }
    }

    public String getVersion()
    {
        if(version==null) {
            version = MuleManager.getConfiguration().getProductVersion();
            if(version==null) {
                version = "Mule Version Info Not Set";
            }
        }
        return version;
    }

    public String getVendor()
    {
        if(vendor==null) {
            vendor = MuleManager.getConfiguration().getVendorName();
            if(vendor==null) {
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

    public boolean isSynchronous()
    {
        return MuleManager.getConfiguration().isSynchronous();
    }

    public void setSynchronous(boolean synchronous)
    {
        MuleManager.getConfiguration().setSynchronous(synchronous);
    }

    public int getSynchronousEventTimeout()
    {
        return MuleManager.getConfiguration().getSynchronousEventTimeout();
    }

    public void setSynchronousEventTimeout(int synchronousEventTimeout)
    {
        MuleManager.getConfiguration().setSynchronousEventTimeout(synchronousEventTimeout);
    }

    public boolean isSynchronousReceive()
    {
        return MuleManager.getConfiguration().isSynchronousReceive();
    }

    public boolean isRecoverableMode()
    {
        return MuleManager.getConfiguration().isRecoverableMode();
    }

    public String getWorkingDirectoy()
    {
        return MuleManager.getConfiguration().getWorkingDirectoy();
    }

    public String[] getConfigResources()
    {
        return MuleManager.getConfiguration().getConfigResources();
    }

    public String getServerUrl()
    {
        return MuleManager.getConfiguration().getServerUrl();
    }
}
