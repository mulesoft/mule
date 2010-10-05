/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;


/**
 * Extend this class instead of FunctionalTestCase to add dynamic port support to
 * your tests. The test will need to only implement 'getNumPortsToFind' to tell this
 * class how many free test ports to find.
 */
public abstract class DynamicPortTestCase extends FunctionalTestCase
{
    //private ArrayList<Integer> ports = null;

    protected abstract int getNumPortsToFind();

    public DynamicPortTestCase()
    {
        super();
        // each test class sets the number of free ports to find 
        numPorts = getNumPortsToFind();
        //ports = findFreePorts(getNumPortsToFind());
        // this will propagate to the mule configuration
        //setPortProperties();
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // see if the ports are available, tests should fail if they are not
        //checkPorts(false, "SETUP");
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        // make sure that the ports have been freed. It's not a fatal error, but we
        // want to track down why it's not being released
        checkPorts(false, "TEARDOWN");
        
        //find a new set of ports so the next test does not fail, regardless of the current ports not being available
        //ports = findFreePorts(getNumPortsToFind());
        // this will propagate to the mule configuration
        //setPortProperties();        
    }
}
