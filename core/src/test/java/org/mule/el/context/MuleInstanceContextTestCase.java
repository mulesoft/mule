/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.context;

import org.mule.config.MuleManifest;

import java.net.UnknownHostException;

import junit.framework.Assert;

import org.junit.Test;

public class MuleInstanceContextTestCase extends AbstractELTestCase
{
    public MuleInstanceContextTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void version() throws UnknownHostException
    {
        Assert.assertEquals(MuleManifest.getProductVersion(), evaluate("mule.version"));
    }

    public void assignValueToMuleVersion()
    {
        assertFinalProperty("mule.version='1'");
    }

    @Test
    public void home() throws UnknownHostException
    {
        Assert.assertEquals(muleContext.getConfiguration().getMuleHomeDirectory(), evaluate("mule.home"));
    }

    public void assignValueToHomeDir()
    {
        assertFinalProperty("mule.home='1'");
    }

    @Test
    public void clusterId() throws UnknownHostException
    {
        Assert.assertEquals(muleContext.getClusterId(), evaluate("mule.clusterId"));
    }

    public void assignValueToClusterId()
    {
        assertFinalProperty("mule.clusterId='1'");
    }

    @Test
    public void nodeId() throws UnknownHostException
    {
        Assert.assertEquals(muleContext.getClusterNodeId(), evaluate("mule.nodeId"));
    }

    public void assignValueToNodeId()
    {
        assertFinalProperty("mule.nodeId='1'");
    }

}
