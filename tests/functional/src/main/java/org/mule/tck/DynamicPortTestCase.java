/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
