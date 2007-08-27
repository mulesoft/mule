/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

import java.util.Collection;
import java.util.HashSet;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class InOutState implements ServerState
{

    private NamedPayload payload;
    private CountDownLatch started = new CountDownLatch(1);
    private CountDownLatch received = new CountDownLatch(1);


    private boolean isPayloadAvailable()
    {
        return null != payload;
    }

    public NamedPayload getDownload(String name)
    {
        if (isPayloadAvailable() && (name == null || name.equals(payload.getName())))
        {
            NamedPayload download = payload;
            payload = null;
            return download;
        }
        else
        {
            return null;
        }
    }

    public Collection getDownloadNames()
    {
        Collection names = new HashSet();
        if (isPayloadAvailable())
        {
            names.add(payload.getName());
        }
        return names;
    }

    public void pushLastUpload(NamedPayload payload)
    {
        this.payload = payload;
        received.countDown();
    }

    public void started()
    {
        started.countDown();
    }

    public void awaitStart(long ms) throws InterruptedException
    {
        started.await(ms, TimeUnit.MILLISECONDS);
    }

    public NamedPayload awaitUpload(long ms) throws InterruptedException
    {
        received.await(ms, TimeUnit.MILLISECONDS);
        return payload;
    }

}
