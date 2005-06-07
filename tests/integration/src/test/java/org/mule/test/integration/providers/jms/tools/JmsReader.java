/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.test.integration.providers.jms.tools;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JmsReader
{

    private Log logger = LogFactory.getLog(JmsReader.class);
    private Session session = null;
    private Connection cnn = null;
    private MessageConsumer consumer = null;
    private boolean isQueue = false;
    private int msgCount = 0;
    private int noOfMessages = 1;
    private boolean shutdown = false;

    public JmsReader(String jndiProps, boolean isQueue)
    {
        this.isQueue = isQueue;
        try {
            FileInputStream fis = new FileInputStream(new File(jndiProps));
            Properties p = new Properties();
            p.load(fis);
            fis.close();
            String cnnFactoryName = p.getProperty("connectionFactoryJNDIName");
            if (cnnFactoryName == null) {
                throw new Exception("You must set the property connectionFactoryJNDIName "
                        + "in the JNDI property file");
            }
            Context ctx = new InitialContext(p);

            if (isQueue) {
                QueueConnectionFactory qcf = (QueueConnectionFactory) ctx.lookup(cnnFactoryName);
                cnn = qcf.createQueueConnection();
                session = ((QueueConnection) cnn).createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            } else {
                TopicConnectionFactory tcf = (TopicConnectionFactory) ctx.lookup(cnnFactoryName);
                cnn = tcf.createTopicConnection();
                session = ((TopicConnection) cnn).createTopicSession(false, 0);
            }
            cnn.start();
        } catch (Exception e) {
            logger.error("Initialisation failed: " + e, e);
            System.exit(0);
        }
    }

    public void read(String destination, int noOfMessages, boolean block)
    {
        try {
            this.noOfMessages = noOfMessages;
            if (isQueue) {
                Queue queue = ((QueueSession) session).createQueue(destination);
                consumer = ((QueueSession) session).createReceiver(queue);
            } else {
                Topic topic = ((TopicSession) session).createTopic(destination);
                consumer = ((TopicSession) session).createSubscriber(topic);
            }
            cnn.start();

            if (!block) {
                ReceiverThread receiver = new ReceiverThread();
                receiver.start();
            } else {
                Message message = null;
                while (noOfMessages > msgCount) {
                    try {
                        message = consumer.receive(1000);
                        if (message != null) {

                            logger.info("onMessage: Message recieved");

                            if (message instanceof TextMessage) {
                                logger.info("Message Content is: " + ((TextMessage) message).getText());
                            } else {
                                logger.info("Message is: " + message.toString());
                            }
                            msgCount++;
                        } else {
                            logger.debug("Receive timed out");
                        }

                    } catch (JMSException e) {
                        logger.error("failed: " + e, e);
                        System.exit(0);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Initialisation failed: " + e, e);
            close();
            System.exit(0);
        }
    }

    public void close()
    {
        try {
            shutdown = true;
            consumer.close();
            cnn.close();
        } catch (JMSException e) {
            logger.error("Exception occurred while closing: " + e);
        }
    }

    public static void main(String[] args)
    {
        int noOfMessages = 1;
        boolean queue = false;
        if (args.length == 5) {
            if (args[2].equals("-q"))
                queue = true;
            noOfMessages = Integer.parseInt(args[3]);
            boolean isBlock = Boolean.valueOf(args[4]).booleanValue();

            JmsReader Jms = new JmsReader(args[0], queue);
            Jms.read(args[1], noOfMessages, isBlock);
        } else {
            usage();
        }

    }

    public static void usage()
    {
        System.out.println("Arg 1: JNDI property file");
        System.out.println("Arg 2: Destination name");
        System.out.println("Arg 3: -q for Queue or -t for topic");
        System.out.println("Arg 4: Number of messages");
        System.out.println("Arg 5: true to block before message is received otherwise false");
        System.exit(0);
    }

    private class ReceiverThread extends Thread
    {

        public ReceiverThread()
        {
            super("JmsReader.Receiver");
        }

        public void run()
        {
            Message message = null;
            while (noOfMessages >= msgCount || !shutdown) {
                try {
                    message = consumer.receive(1000);
                    if (message != null) {

                        logger.info("\n***** onMessage: Message recieved");

                        if (message instanceof TextMessage) {
                            logger.info("Message Content is: " + ((TextMessage) message).getText());
                        } else {
                            logger.info("Message is: " + message.toString());
                        }
                        msgCount++;
                    } else {
                        logger.debug("Receive timed out");
                    }
                } catch (JMSException e) {
                    logger.error("failed: " + e, e);
                    System.exit(0);
                }
            }
            close();
        }
    }

}
