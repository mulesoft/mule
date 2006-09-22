/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.Utility;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class JmsWriter
{

    Log logger = LogFactory.getLog(JmsWriter.class);

    Session session = null;
    Connection cnn = null;
    MessageProducer producer = null;
    boolean isQueue = false;

    public JmsWriter(String jndiProps, boolean isQueue)
    {
        this.isQueue = isQueue;
        try {
            FileInputStream fis = new FileInputStream(new File(jndiProps));
            Properties p = new Properties();
            p.load(fis);
            fis.close();
            String cnnFactoryName = p.getProperty("connectionFactoryJNDIName");
            if (cnnFactoryName == null) {

                throw new Exception("You must set the property connectionFactoryJNDIName in the JNDI property file");
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

    public void write(String data, String destination, int noOfMessages)
    {
        try {

            if (isQueue) {
                Queue queue = ((QueueSession) session).createQueue(destination);
                producer = ((QueueSession) session).createSender(queue);
            } else {
                Topic topic = ((TopicSession) session).createTopic(destination);
                producer = ((TopicSession) session).createPublisher(topic);
            }

            TextMessage tm;
            logger.info("Sending message: " + data + " on " + destination);
            for (int i = 0; i < noOfMessages; i++) {
                tm = session.createTextMessage(data);
                if (isQueue) {
                    ((QueueSender) producer).send(tm);
                } else {
                    ((TopicPublisher) producer).publish(tm);
                }
            }
            logger.info(noOfMessages + " message(s) sent successfully");
        } catch (Exception e) {
            logger.error("Initialisation failed: " + e, e);
            try {
                cnn.close();
            } catch (JMSException e1) {
                System.exit(0);
            }
            System.exit(0);
        }
    }

    public void close()
    {
        try {
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
            noOfMessages = Integer.parseInt(args[4]);
            if (args[3].equals("-q"))
                queue = true;
            String data = args[0];
            if (data.endsWith(".xml")) {
                try {
                    data = Utility.fileToString(data);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
            JmsWriter Jms = new JmsWriter(args[1], queue);

            Jms.write(data, args[2], noOfMessages);
            Jms.close();
        } else {
            usage();
        }

    }

    public static void usage()
    {
        System.out.println("Arg 1: Message data string or XML file");
        System.out.println("Arg 2: JNDI property file");
        System.out.println("Arg 3: Destination name");
        System.out.println("Arg 4: -q for Queue or -t for topic");
        System.out.println("Arg 5: Number of messages");
        System.exit(0);
    }

}
