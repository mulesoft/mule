/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
