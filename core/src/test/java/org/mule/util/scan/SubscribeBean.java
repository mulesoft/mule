/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan;

import org.mule.util.scan.annotations.Marker;


/**
 * Repesents a subscribe bean in a different package from where the test is run
 */
public class SubscribeBean
{
    @Marker("foo")
    public String doSomething(Object data)
    {
        return null;
    }
}
