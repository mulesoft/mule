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
        else
        {
            throw new IllegalArgumentException("session type " + session.getClass() + " no supported as reusable");
        }
    }

}
