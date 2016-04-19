/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicSession;

public class ReusableSessionWrapperFactory
{
    public static Session createWrapper(Session session)
    {
        if (session instanceof TopicSession)
        {
            return new ReusableTopicSessionWrapper((TopicSession) session);
        }
        else if (session instanceof QueueSession)
        {
            return new ReusableQueueSessionWrapper((javax.jms.QueueSession) session);
        }
        else if (session instanceof Session)
        {
            return new ReusableSessionWrapper(session);
        }
        else
        {
            throw new IllegalArgumentException("session type " + session.getClass() + " no supported as reusable");
        }
    }

}
