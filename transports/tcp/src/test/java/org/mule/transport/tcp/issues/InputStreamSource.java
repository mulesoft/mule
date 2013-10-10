/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.issues;

import org.mule.transport.tcp.integration.AbstractStreamingCapacityTestCase;
import org.mule.transport.tcp.integration.BigInputStream;

public class InputStreamSource
{

    public static final long SIZE = 10 * AbstractStreamingCapacityTestCase.ONE_MB;

    // whether this is declared to return Object or InputStream makes no difference
    public Object doSomething(Object request)
    {
        return new BigInputStream(SIZE, 10);
    }

}
