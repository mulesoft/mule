/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;

/**
 * An interface used for reading and writing session information to and from the
 * current message.
 */
public interface SessionHandler
{

    void storeSessionInfoToMessage(MuleSession session, MuleMessage message) throws MuleException;

    /**
     * @deprecated Use retrieveSessionInfoFromMessage(MuleMessage message) instead
     */
    @Deprecated
    void retrieveSessionInfoFromMessage(MuleMessage message, MuleSession session) throws MuleException;

    MuleSession retrieveSessionInfoFromMessage(MuleMessage message) throws MuleException;

    /**
     * The property name of the session id to use when creating the Mule session. by
     * default the property name "ID" will be used. If no property was set on the
     * session called "ID" a session id will be automatically generated
     * 
     * @return the property name of the session id that is set on the session
     * @deprecated This method is no longer needed and will be removed in the next major release
     */
    @Deprecated
    String getSessionIDKey();
}
