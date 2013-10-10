/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
