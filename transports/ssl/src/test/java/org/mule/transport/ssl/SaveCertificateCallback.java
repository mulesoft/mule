/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl;

import org.mule.tck.functional.EventCallback;
import org.mule.api.MuleEventContext;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class SaveCertificateCallback implements EventCallback
{

    private AtomicReference certificates;
    private AtomicBoolean called;

    public SaveCertificateCallback()
    {
        clear();
    }

    public void eventReceived(MuleEventContext context, Object component) throws Exception
    {
        // putting a Thread.sleep here doesn't make this less reliable
        // surely it would if it was thread scribbling?
        certificates.set(context.getMessage().getProperty(SslConnector.LOCAL_CERTIFICATES));
        called.set(true);
    }

    public void clear()
    {
        certificates = new AtomicReference();
        called = new AtomicBoolean(false);
    }

    public boolean isCalled()
    {
        return called.get();
    }

    public Object getCertificates()
    {
        return certificates.get();
    }

}
