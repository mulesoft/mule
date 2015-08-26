/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
