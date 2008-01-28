/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;


import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JmsXAAlwaysBeginTestCase extends AbstractJmsFunctionalTestCase
{

    private static final List committedTx = new CopyOnWriteArrayList();
    private static final List rolledbackTx = new CopyOnWriteArrayList();
    protected static final Log logger = LogFactory.getLog(JmsXAAlwaysBeginTestCase.class);

    protected String getConfigResources()
    {
        return "providers/activemq/jms-xa-tx-ALWAYS_BEGIN.xml";
    }

    public void testAlwaysBeginTx() throws Exception
    {
        CurrentScenario scenarioNoTx = new CurrentScenario(DEFAULT_INPUT_MQ_QUEUE_NAME, DEFAULT_OUTPUT_MQ_QUEUE_NAME, true);
        send(scenarioNoTx);
        receive(scenarioNoTx);
        receive(scenarioNoTx);

        scenarioNoTx.setRecieve(false);
        receive(scenarioNoTx);
        assertEquals(committedTx.size(), 0);
        assertEquals(rolledbackTx.size(), 2);
    }


    public static class TestRollbackComponent
    {

        public Object processObject(Object a) throws Exception
        {
            logger.debug("TestRollbackComponent " + a);
            TestResource res = new TestResource();
            Transaction currentTrans = muleContext.getTransactionManager().getTransaction();
            currentTrans.enlistResource(res);
            currentTrans.setRollbackOnly();
            return DEFAULT_OUTPUT_MESSAGE;
        }
    }

    public static class TestResource implements XAResource
    {

        public void commit(Xid id, boolean onePhase) throws XAException
        {
            committedTx.add(id);
            logger.debug("XA_COMMIT[" + id + "]");
        }

        public void end(Xid xid, int flags) throws XAException
        {
            logger.debug("XA_END[" + xid + "] Flags=" + flags);
        }

        public void forget(Xid xid) throws XAException
        {
            logger.debug("XA_FORGET[" + xid + "]");
        }

        public int getTransactionTimeout() throws XAException
        {
            return (_timeout);
        }

        public boolean isSameRM(XAResource xares) throws XAException
        {
            return (xares.equals(this));
        }

        public int prepare(Xid xid) throws XAException
        {
            logger.debug("XA_PREPARE[" + xid + "]");

            return (XA_OK);
        }

        public Xid[] recover(int flag) throws XAException
        {
            logger.debug("RECOVER[" + flag + "]");
            return (null);
        }

        public void rollback(Xid xid) throws XAException
        {
            rolledbackTx.add(xid);
            logger.debug("XA_ROLLBACK[" + xid + "]");
        }

        public boolean setTransactionTimeout(int seconds) throws XAException
        {
            _timeout = seconds;
            return (true);
        }

        public void start(Xid xid, int flags) throws XAException
        {
            logger.debug("XA_START[" + xid + "] Flags=" + flags);
        }

        protected int _timeout = 0;
    }

    class CurrentScenario extends AbstractScenario
    {

        private String inputQueue;
        private String outputQueue;
        private boolean recieve = true;

        public CurrentScenario(String inputQueue, String outputQueue, boolean recieve)
        {
            this.inputQueue = inputQueue;
            this.outputQueue = outputQueue;
        }

        public String getInputQueue()
        {
            return inputQueue;
        }

        public String getOutputQueue()
        {
            return outputQueue;
        }

        public boolean isRecieve()
        {
            return recieve;
        }

        public void setRecieve(boolean recieve)
        {
            this.recieve = recieve;
        }

        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(TIMEOUT);
            if (isRecieve())
            {
                assertNotNull(message);
                assertTrue(TextMessage.class.isAssignableFrom(message.getClass()));
                assertEquals(((TextMessage) message).getText(), DEFAULT_OUTPUT_MESSAGE);
            }
            else
            {
                assertNull(message);
            }
            return message;
        }

        public boolean isTransacted()
        {
            return false;
        }

        protected void applyTransaction(Session session) throws JMSException
        {
            //do nothink
        }
    }


}