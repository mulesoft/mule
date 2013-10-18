/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;

import org.mule.api.MuleEventContext;
import org.mule.tck.functional.EventCallback;

import java.security.cert.Certificate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SaveCertificatesCallback implements EventCallback
{
    // volatile since this is a thread-safe collection (see holger)
    private volatile List<Certificate[]> certificates;

    public SaveCertificatesCallback()
    {
        super();
        clear();
    }

    public void eventReceived(MuleEventContext context, Object component) throws Exception
    {
        // putting a Thread.sleep here doesn't make this less reliable
        // surely it would if it was thread scribbling?
        Thread.sleep(100);

        Certificate[] certs = context.getMessage().getOutboundProperty(SslConnector.LOCAL_CERTIFICATES);
        certificates.add(certs);
    }

    public void clear()
    {
        certificates = Collections.synchronizedList(new LinkedList<Certificate[]>());
    }

    public List<Certificate[]> getCertificates()
    {
        return certificates;
    }
}
