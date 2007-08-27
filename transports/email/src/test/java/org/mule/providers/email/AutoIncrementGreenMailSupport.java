/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class AutoIncrementGreenMailSupport extends AbstractGreenMailSupport
{

    // something odd happening here?  50006 seems to have failed a
    // couple of times?
    public static final int INITIAL_SERVER_PORT = 50007;
    // large enough to jump away from a group of related ports
    public static final int PORT_INCREMENT = 17;
    private static final AtomicInteger nextPort = new AtomicInteger(INITIAL_SERVER_PORT);

    protected int nextPort()
    {
        return nextPort.addAndGet(PORT_INCREMENT);
    }

}
