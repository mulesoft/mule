/*
 * $Header$
 * $Revision$
 * $Date$
 * -----------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.providers.jms;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.FullConstraintMatcher;
import com.mockobjects.dynamic.Mock;

import org.mule.tck.AbstractMuleTestCase;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 *
 * $Id$
 */
public class Jms11SupportTestCase extends AbstractMuleTestCase
{
    public void testNoLocalCalledForDurableTopic() throws Exception
    {
        Jms11Support jmsSupport = new Jms11Support(new JmsConnector(), null, false, false);

        Mock mockTopic = new Mock(Topic.class);
        Topic topic = (Topic) mockTopic.proxy();

        String durableName = "durableName";
        boolean noLocal = true;

        FullConstraintMatcher matcher = new FullConstraintMatcher(new Constraint[] {
                C.eq(topic), C.eq(durableName), C.IS_NULL, C.IS_TRUE
        });

        Mock mockSession = new Mock(Session.class);
        mockSession.expect("createDurableSubscriber", matcher);

        jmsSupport.createConsumer((Session) mockSession.proxy(), topic, null, noLocal, durableName, true);

        mockTopic.verify();
        mockSession.verify();
    }

    public void testNoLocalCalledForNonDurableTopic() throws Exception
    {
        Jms11Support jmsSupport = new Jms11Support(new JmsConnector(), null, false, false);

        Mock mockTopic = new Mock(Topic.class);
        Topic topic = (Topic) mockTopic.proxy();

        boolean noLocal = true;

        FullConstraintMatcher matcher = new FullConstraintMatcher(new Constraint[] {
                C.eq(topic), C.IS_NULL, C.IS_TRUE
        });

        Mock mockSession = new Mock(Session.class);
        mockSession.expect("createConsumer", matcher);

        jmsSupport.createConsumer((Session) mockSession.proxy(), topic, null, noLocal, null, true);

        mockTopic.verify();
        mockSession.verify();
    }

    public void testNoLocalNotCalledForQueue() throws Exception
    {
        Jms11Support jmsSupport = new Jms11Support(new JmsConnector(), null, false, false);

        Mock mockQueue = new Mock(Queue.class);
        Queue queue = (Queue) mockQueue.proxy();

        boolean noLocal = true;

        FullConstraintMatcher matcher = new FullConstraintMatcher(new Constraint[] {
                C.eq(queue), C.IS_NULL
        });

        Mock mockSession = new Mock(Session.class);
        mockSession.expect("createConsumer", matcher);

        jmsSupport.createConsumer((Session) mockSession.proxy(), queue, null, noLocal, null, false);

        mockQueue.verify();
        mockSession.verify();
    }

    public void testDurableWithQueueThrowsException() throws Exception
    {
        Jms11Support jmsSupport = new Jms11Support(new JmsConnector(), null, false, false);

        Mock mockQueue = new Mock(Queue.class);
        Queue queue = (Queue) mockQueue.proxy();

        String durableName = "durableName";
        boolean noLocal = true;

        Mock mockSession = new Mock(Session.class);

        try {
            jmsSupport.createConsumer((Session) mockSession.proxy(), queue, null, noLocal, durableName, false);
        } catch (JMSException jmsex) {
            // expected
            assertEquals("Wrong exception text.",
                    "A durable subscriber name was set but the destination was not a Topic",
                    jmsex.getMessage());
        }

        mockQueue.verify();
        mockSession.verify();
    }

}
