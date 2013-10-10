/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email;

import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;

public class SessionDetails
{

    private Session session;
    private URLName url;
    
    public SessionDetails(Session session, URLName url)
    {
        this.session = session;
        this.url = url;
    }
    
    public Session getSession()
    {
        return session;
    }
    
    public URLName getUrl()
    {
        return url;
    }
    
    public Store newStore() throws NoSuchProviderException
    {
        return session.getStore(url);
    }
    
    public Transport newTransport() throws NoSuchProviderException
    {
        return session.getTransport(url);
    }
    
}
