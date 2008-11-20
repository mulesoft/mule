/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;


import org.mule.lifecycle.AlreadyInitialisedException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

import junit.framework.Assert;

/**
 * Tests that lifecycle methods on a connector are not processed more than once.  
 * @see MULE-3062
 */
public class ConnectorLifecycleTestCase extends AbstractMuleTestCase 
{
    private TestConnector connector;

	public void doSetUp() throws Exception 
	{
		connector = new TestConnector();
		connector.setMuleContext(muleContext);
        connector.initialise();
	}

	public void doTearDown() throws Exception 
	{
	    connector = null;
	}

	/**
	 * This test ensures that the connector is only initialised once even on a
	 * direct initialisation (not through Mule).
	 */
	public void testDoubleInitialiseConnector() throws Exception 
	{
	    // Note: the connector was already initialized once during doSetUp()
	    
		// Initialising the connector should leave it disconnected.
		assertEquals(1, connector.getInitialiseCount());
		assertEquals(0, connector.getConnectCount());
		assertEquals(0, connector.getStartCount());
		assertEquals(0, connector.getStopCount());
		assertEquals(0, connector.getDisconnectCount());
		assertEquals(0, connector.getDisposeCount());

		// Initialising the connector again should not throw an exception.
		try {
			System.out.println("Initialising connector again...");
			connector.initialise();
			Assert.fail("Expected AlreadyInitialisedException not thrown.");
		} catch (AlreadyInitialisedException ex) {
			// ignore since expected
		}
	}

	/**
	 * This test ensures that the connector is only started once even on a
	 * direct restart (not through Mule).
	 */
	public void testDoubleStartConnector() throws Exception 
	{
		// Starting the connector should leave it uninitialised,
		// but connected and started.
		System.out.println("Starting connector...");
		connector.start();
		assertEquals(1, connector.getInitialiseCount());
		assertEquals(1, connector.getConnectCount());
		assertEquals(1, connector.getStartCount());
		assertEquals(0, connector.getStopCount());
		assertEquals(0, connector.getDisconnectCount());
		assertEquals(0, connector.getDisposeCount());

		// Starting the connector against should not affect it.
		System.out.println("Starting connector again...");
		connector.start();
		assertEquals(1, connector.getInitialiseCount());
		assertEquals(1, connector.getConnectCount());
		assertEquals(1, connector.getStartCount());
		assertEquals(0, connector.getStopCount());
		assertEquals(0, connector.getDisconnectCount());
		assertEquals(0, connector.getDisposeCount());
	}

	/**
	 * This test ensures that the connector is only stopped once even on a
	 * direct restop (not through Mule).
	 */
	public void testDoubleStopConnector() throws Exception 
	{
		// Starting the connector should leave it uninitialised,
		// but connected and started.
		System.out.println("Starting connector...");
		connector.start();
		assertEquals(1, connector.getInitialiseCount());
		assertEquals(1, connector.getConnectCount());
		assertEquals(1, connector.getStartCount());
		assertEquals(0, connector.getStopCount());
		assertEquals(0, connector.getDisconnectCount());
		assertEquals(0, connector.getDisposeCount());

		assertTrue(connector.isStarted());
		
		// Stopping the connector should stop and disconnect it.
		System.out.println("Stopping connector...");
		connector.stop();
		assertEquals(1, connector.getInitialiseCount());
		assertEquals(1, connector.getConnectCount());
		assertEquals(1, connector.getStartCount());
		assertEquals(1, connector.getStopCount());
		assertEquals(1, connector.getDisconnectCount());
		assertEquals(0, connector.getDisposeCount());

		// Stopping the connector again should not affect it.
		System.out.println("Stopping connector again...");
		connector.stop();
		assertEquals(1, connector.getInitialiseCount());
		assertEquals(1, connector.getConnectCount());
		assertEquals(1, connector.getStartCount());
		assertEquals(1, connector.getStopCount());
		assertEquals(1, connector.getDisconnectCount());
		assertEquals(0, connector.getDisposeCount());
	}

	/**
	 * This test ensures that the connector is only disposed once even on a
	 * direct disposal (not through Mule).
	 */
	public void testDoubleDisposeConnectorStartStop() throws Exception 
	{
        System.out.println("Starting connector...");
        connector.start();
        assertTrue(connector.isStarted());
        
        System.out.println("Stopping connector...");
        connector.stop();
        assertFalse(connector.isStarted());
        
		// Disposing the connector should leave it uninitialised.
		System.out.println("Disposing connector...");
		connector.dispose();
		assertEquals(1, connector.getInitialiseCount());
		assertEquals(1, connector.getConnectCount());
		assertEquals(1, connector.getStartCount());
		assertEquals(1, connector.getStopCount());
		assertEquals(1, connector.getDisconnectCount());
		assertEquals(1, connector.getDisposeCount());

		// Disposing the connector again should not affect it.
		System.out.println("Disposing connector again...");
		connector.dispose();
		assertEquals(1, connector.getInitialiseCount());
		assertEquals(1, connector.getConnectCount());
		assertEquals(1, connector.getStartCount());
		assertEquals(1, connector.getStopCount());
		assertEquals(1, connector.getDisconnectCount());
		assertEquals(1, connector.getDisposeCount());	
	}

    /**
     * This test ensures that the connector is only disposed once even on a
     * direct disposal (not through Mule).
     */
    public void testDoubleDisposeConnectorStartOnly() throws Exception 
    {
        System.out.println("Starting connector...");
        connector.start();
        assertTrue(connector.isStarted());
        
        // Disposing the connector should leave it uninitialised.
        System.out.println("Disposing connector...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        // dispose() implicitly calls stop()
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        // Disposing the connector again should not affect it.
        System.out.println("Disposing connector again...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        // dispose() implicitly calls stop()
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());   
    }

    /**
     * This test ensures that the connector is only disposed once even on a
     * direct disposal (not through Mule).
     */
    public void testDoubleDisposeConnector() throws Exception 
    {
        // Disposing the connector should leave it uninitialised.
        System.out.println("Disposing connector...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        // Disposing the connector again should not affect it.
        System.out.println("Disposing connector again...");
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());   
    }
}
