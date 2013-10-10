/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
