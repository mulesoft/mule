/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.service;

/**
 * Interface for TestComponent (to make it easier to host the service on Axis)
 * 
 * @author Alan Cassar
 * 
 */
public interface ITestComponent
{
    public String receive(String message) throws Exception;
    public String throwsException(String message) throws Exception;
}
