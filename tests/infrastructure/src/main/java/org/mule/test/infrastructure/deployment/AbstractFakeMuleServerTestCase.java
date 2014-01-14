/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.deployment;

import org.mule.MuleCoreExtension;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class AbstractFakeMuleServerTestCase extends AbstractMuleTestCase
{

    @Rule
    public TemporaryFolder muleHome = new TemporaryFolder();

    protected FakeMuleServer muleServer;

    protected List<MuleCoreExtension> getCoreExtensions()
    {
        return new LinkedList<MuleCoreExtension>();
    }

    @Before
    public void setUp() throws Exception
    {
        muleServer = new FakeMuleServer(muleHome.getRoot().getAbsolutePath(), getCoreExtensions());
    }

    @After
    public void tearDown() throws Exception
    {
        if (muleServer != null)
        {
            muleServer.stop();
            muleServer = null;
        }
    }
}
