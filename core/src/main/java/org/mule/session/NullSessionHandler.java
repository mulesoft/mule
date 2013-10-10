/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.session;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.transport.SessionHandler;

/**
 * A session handler that ignores any session information
 */
public class NullSessionHandler implements SessionHandler
{
    public MuleSession retrieveSessionInfoFromMessage(MuleMessage message) throws MuleException
    {
        return null;
    }

    /**
     * @deprecated Use retrieveSessionInfoFromMessage(MuleMessage message) instead
     */
    @Deprecated
    public void retrieveSessionInfoFromMessage(MuleMessage message, MuleSession session) throws MuleException
    {
        retrieveSessionInfoFromMessage(message);
    }

    public void storeSessionInfoToMessage(MuleSession session, MuleMessage message) throws MuleException
    {
        // noop
    }

    /**
     * The property name of the session id to use when creating the Mule session. by
     * default the property name "ID" will be used. If no property was set on the
     * session called "ID" a session id will be automatically generated
     * 
     * @return the property name of the session id that is set on the session
     */
    public String getSessionIDKey()
    {
        return "ID";
    }
}
