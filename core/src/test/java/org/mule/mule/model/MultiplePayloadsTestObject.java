/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.mule.model;

/**
 * A test object with multiple signatures matching a payload for the discovery to fail.
 */
public class MultiplePayloadsTestObject
{
    public MultiplePayloadsTestObject()
    {
        // nothing to do
    }

    public void someBusinessMethod(String parameter)
    {
        // nothing to do
    }

    public void someSetter(String otherStringParameter)
    {
        // nothing to do
    }

    public void someOtherBusinessMethod(String parameter, String otherParameter)
    {
        // nothing to do
    }
}
