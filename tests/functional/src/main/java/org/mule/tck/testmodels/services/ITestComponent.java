/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.services;

/**
 * Interface for TestComponent (to make it easier to host the service on Axis)
 */
public interface ITestComponent
{
    public String receive(String message) throws Exception;

    public String throwsException(String message) throws Exception;
}
