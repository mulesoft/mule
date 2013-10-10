/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ssl;

import org.mule.api.MuleEventContext;
import org.mule.tck.functional.EventCallback;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SaveCertificateCallback implements EventCallback
{
    private AtomicReference<Object> certificates;
    private AtomicBoolean called;

    public SaveCertificateCallback()
    {
        clear();
    }

    @Override
    public void eventReceived(MuleEventContext context, Object component) throws Exception
    {
        certificates.set(context.getMessage().getOutboundProperty(SslConnector.LOCAL_CERTIFICATES));
        called.set(true);
    }

    public void clear()
    {
        certificates = new AtomicReference<Object>();
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
