/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import org.mule.providers.tcp.integration.AbstractStreamingCapacityTestCase;
import org.mule.providers.tcp.integration.BigInputStream;

public class InputStreamSource
{

    public static final long SIZE = 10 * AbstractStreamingCapacityTestCase.ONE_MB;

    // whether this is declared to return Object or InputStream makes no difference
    public Object doSomething(Object request)
    {
        return new BigInputStream(SIZE, 10);
    }

}
