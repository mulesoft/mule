/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;


/**
 * Extend this class instead of FunctionalTestCase to add dynamic port support to
 * your tests. The test will need to only implement 'getNumPortsToFind' to tell this
 * class how many free test ports to find.
 *
 * @deprecated: use {@link org.mule.tck.junit4.rule.DynamicPort} in a JUnit 4 test.
 */
@Deprecated
public abstract class DynamicPortTestCase extends FunctionalTestCase
{
    protected abstract int getNumPortsToFind();

    public DynamicPortTestCase()
    {
        super();

        // each test class sets the number of free ports to find
        numPorts = getNumPortsToFind();
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        // make sure that the ports have been freed. It's not a fatal error, but we
        // want to track down why it's not being released
        PortUtils.checkPorts(false, "TEARDOWN", getPorts());
    }
}
