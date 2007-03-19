/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

/**
 * <code>TestConnector</code> use a mock connector
 */
public class TestConnector2 extends TestConnector
{

    public String getProtocol()
    {
        return "test2";
    }

}