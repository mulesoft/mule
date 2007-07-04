/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mule.model;

/**
 * A test object with multiple signatures matching a payload for the discovery to fail.
 */
public class MultiplePayloadsTestObject
{

    public void someBusinessMethod(String parameter)
    {
        // nothing to do
    }

    public void someSetter(String otherStringParameter)
    {
        // nothing to do
    }

}
