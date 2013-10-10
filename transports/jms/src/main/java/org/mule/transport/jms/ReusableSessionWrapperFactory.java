/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

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
