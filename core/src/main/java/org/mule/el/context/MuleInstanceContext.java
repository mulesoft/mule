/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.context;

import org.mule.api.MuleContext;
import org.mule.config.MuleManifest;

/**
 * Expose information about the Mule instance:
 * 
 * <li> <b>clusterid</b>       <i>Cluster ID</i>
 * <li> <b>home</b>            <i>Home directory</i>
 * <li> <b>nodeid</b>          <i>Cluster Node ID</i>
 * <li> <b>version</b>         <i>Mule Version</i>
 */
public class MuleInstanceContext
{

    private MuleContext muleContext;

    public MuleInstanceContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public static String getVersion()
    {
        return MuleManifest.getProductVersion();
    }

    public String getClusterId()
    {
        return muleContext.getClusterId();
    }

    public int getNodeId()
    {
        return muleContext.getClusterNodeId();
    }

    public String getHome()
    {
        return muleContext.getConfiguration().getMuleHomeDirectory();
    }

}
