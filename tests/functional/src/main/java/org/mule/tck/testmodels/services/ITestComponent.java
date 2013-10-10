/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
