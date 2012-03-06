/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.context;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.config.MuleManifest;
import org.mule.el.AbstractELTestCase;

import java.net.UnknownHostException;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;

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

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToMuleVersion()
    {
        evaluate("mule.version='1'", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void homeDir() throws UnknownHostException
    {
        Assert.assertEquals(muleContext.getConfiguration().getMuleHomeDirectory(), evaluate("mule.homedir"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToHomeDir()
    {
        evaluate("mule.homedir='1'", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void clusterId() throws UnknownHostException
    {
        Assert.assertEquals(muleContext.getClusterId(), evaluate("mule.clusterid"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToClusterId()
    {
        evaluate("mule.clusterid='1'", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void nodeId() throws UnknownHostException
    {
        Assert.assertEquals(muleContext.getClusterNodeId(), evaluate("mule.nodeid"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToNodeId()
    {
        evaluate("mule.nodeid='1'", Mockito.mock(MuleMessage.class));
    }

}
