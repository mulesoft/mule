/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
