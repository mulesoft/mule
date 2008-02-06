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

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

public class SaveCertificatesCallback implements EventCallback
{

    // volatile since this is a thread-safe collection (see holger)
    private volatile List certificates;

    public SaveCertificatesCallback()
    {
        clear();
    }

    public void eventReceived(MuleEventContext context, Object component) throws Exception
    {
        // putting a Thread.sleep here doesn't make this less reliable
        // surely it would if it was thread scribbling?
        Thread.sleep(100);
        certificates.add(context.getMessage().getProperty(SslConnector.LOCAL_CERTIFICATES));
    }

    public void clear()
    {
        certificates = Collections.synchronizedList(new LinkedList());
    }

    public List getCertificates()
    {
        return certificates;
    }

}