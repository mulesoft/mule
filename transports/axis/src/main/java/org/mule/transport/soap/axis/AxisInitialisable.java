/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.api.lifecycle.InitialisationException;

import org.apache.axis.handlers.soap.SOAPService;

/**
 * <code>AxisInitialisable</code> can be implemented by a Mule component that will
 * be used as an Axis service to customise the Axis Service object
 */
public interface AxisInitialisable
{
    public void initialise(SOAPService service) throws InitialisationException;
}
