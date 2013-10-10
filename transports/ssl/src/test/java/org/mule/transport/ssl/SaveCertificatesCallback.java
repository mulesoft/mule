/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
