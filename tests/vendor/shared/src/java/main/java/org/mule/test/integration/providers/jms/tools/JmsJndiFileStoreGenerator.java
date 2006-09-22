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

// //*********** Joram specific imports ******************
// import org.objectweb.joram.client.jms.Queue;
// import org.objectweb.joram.client.jms.Topic;
// import org.objectweb.joram.client.jms.admin.AdminModule;
// import org.objectweb.joram.client.jms.tcp.*;
// //********* End Joram specific imports ****************

// //*********** OpenJms specific imports ******************
// import org.exolab.jms.jndi.InitialContextFactory;
// //********* End OpenJms specific imports ****************

// //********* Wave specific imports ****************
// import com.spirit.wave.jms.WaveQueueConnectionFactory;
// import com.spirit.wave.jms.WaveTopicConnectionFactory;
// import com.spirit.wave.jms.WaveXAQueueConnectionFactory;
// import com.spirit.wave.jms.WaveXATopicConnectionFactory;
// //********* End Wave specific imports ****************

// //********* ActiveMq specific imports ****************
import com.sun.jndi.fscontext.RefFSContext;
import com.sun.jndi.fscontext.RefFSContextFactory;

import javax.naming.Context;
import java.io.File;
import java.util.Hashtable;

/**
 * <code>JmsJndiFileStoreGenerator</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JmsJndiFileStoreGenerator
{

    public static void main(String[] args)
    {
        // To use this you need to uncomment the method you wish to use and the
        // imports
        try {
            JmsJndiFileStoreGenerator generator = new JmsJndiFileStoreGenerator();
            generator.generateJoramJndiFileStore();
            generator.generateOpenJmsJndiFileStore();
            generator.generateSpiritWaveJndiFileStore();
            generator.generateUberMQJndiFileStore();
            generator.generateActiveMqJndiFileStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateJoramJndiFileStore() throws Exception
    {
        // RefFSContext ctx = getFileContext("joram");
        // AdminModule.connect("root", "root", 60);
        //
        // Queue inqueue = (Queue) Queue.create(0);
        // Queue outqueue = (Queue) Queue.create(0);
        //
        // Topic topic = (Topic) Topic.create(0);
        //
        // javax.jms.QueueConnectionFactory qcf =
        // QueueTcpConnectionFactory.create("localhost", 16010);
        // javax.jms.TopicConnectionFactory tcf =
        // TopicTcpConnectionFactory.create("localhost", 16010);
        //
        // javax.jms.XAQueueConnectionFactory xaqcf =
        // XAQueueTcpConnectionFactory.create("localhost", 16010);
        // javax.jms.XATopicConnectionFactory xatcf =
        // XATopicTcpConnectionFactory.create("localhost", 16010);
        //
        // //User user = User.create("anonymous", "anonymous", 0);
        //
        // inqueue.setFreeReading();
        // outqueue.setFreeReading();
        // topic.setFreeReading();
        // inqueue.setFreeWriting();
        // outqueue.setFreeWriting();
        // topic.setFreeWriting();
        //
        // ctx.bind("JmsQueueConnectionFactory", qcf);
        // ctx.bind("JmsTopicConnectionFactory", tcf);
        // ctx.bind("XAJmsQueueConnectionFactory", xaqcf);
        // ctx.bind("XAJmsTopicConnectionFactory", xatcf);
        // ctx.bind("in.queue", inqueue);
        // ctx.bind("out.queue", outqueue);
        // ctx.bind("topic", topic);
        // ctx.close();
        //
        // AdminModule.disconnect();
    }

    public void generateOpenJmsJndiFileStore() throws Exception
    {
        // RefFSContext ctx = getFileContext("openjms");
        // //Need to start the server for the store to be available
        // InitialContextFactory icf = new InitialContextFactory();
        // Hashtable t = new Hashtable();
        // t.put(Context.PROVIDER_URL, "tcp://localhost:3035/");
        // Context init =icf.getInitialContext(t);
        //
        // QueueConnectionFactory qcf =
        // (QueueConnectionFactory)init.lookup("JmsQueueConnectionFactory");
        // TopicConnectionFactory tcf =
        // (TopicConnectionFactory)init.lookup("JmsTopicConnectionFactory");
        //
        // //Note XA support is not yet fully implemented in OpenJms
        // //XAQueueConnectionFactory xaqcf =
        // (XAQueueConnectionFactory)init.lookup("XAJmsQueueConnectionFactory");
        // //XATopicConnectionFactory xatcf =
        // (XATopicConnectionFactory)init.lookup("XAJmsTopicConnectionFactory");
        //
        // ctx.bind("JmsQueueConnectionFactory", qcf);
        // ctx.bind("JmsTopicConnectionFactory", tcf);
        // //ctx.bind("XAJmsQueueConnectionFactory", xaqcf);
        // //ctx.bind("XAJmsTopicConnectionFactory", xatcf);
        // ctx.close();
    }

    public void generateActiveMqJndiFileStore() throws Exception
    {
        // Properties props = new Properties();
        // props.load(new
        // FileInputStream("src/providers/jms/src/test/conf/activemq-jndi-connection.properties"));
        // Context ctx = getFileContext("activemq");
        //
        // String brokerUlr = props.getProperty("brokerUrl");
        // String username = props.getProperty("username", null);
        // String password = props.getProperty("password", null);
        // ActiveMQConnectionFactory cf;
        // ActiveMQXAConnectionFactory xacf;
        // if(username!= null) {
        // cf = new ActiveMQConnectionFactory(brokerUlr, username, password);
        // xacf = new ActiveMQXAConnectionFactory(brokerUlr, username,
        // password);
        // } else {
        // cf = new ActiveMQConnectionFactory(brokerUlr);
        // xacf = new ActiveMQXAConnectionFactory(brokerUlr);
        // }
        // ctx.bind("JmsQueueConnectionFactory", cf);
        // ctx.bind("JmsTopicConnectionFactory", cf);
        // ctx.bind("XAJmsQueueConnectionFactory", xacf);
        // ctx.bind("XAJmsTopicConnectionFactory", xacf);
        // ctx.close();

    }

    public void generateSpiritWaveJndiFileStore() throws Exception
    {
        // Properties props = new Properties();
        // props.load(new
        // FileInputStream("src/providers/jms/src/test/conf/spiritwave-jndi-connection.properties"));
        // Context ctx = getFileContext("wave");
        //
        // WaveQueueConnectionFactory qcf = new
        // WaveQueueConnectionFactory(props);
        // WaveTopicConnectionFactory tcf = new
        // WaveTopicConnectionFactory(props);
        // WaveXAQueueConnectionFactory xaqcf = new
        // WaveXAQueueConnectionFactory(props);
        // WaveXATopicConnectionFactory xatcf = new
        // WaveXATopicConnectionFactory(props);
        //
        // ctx.bind("JmsQueueConnectionFactory", qcf);
        // ctx.bind("JmsTopicConnectionFactory", tcf);
        // ctx.bind("XAJmsQueueConnectionFactory", xaqcf);
        // ctx.bind("XAJmsTopicConnectionFactory", xatcf);
        // ctx.close();
    }

    public void generateUberMQJndiFileStore() throws Exception
    {
        // todo
    }

    public RefFSContext getFileContext(String endpointName) throws Exception
    {
        Hashtable props = new Hashtable();
        File f = new File(".");
        String url = f.getCanonicalPath() + "/src/providers/jms/src/test/conf/jndi/" + endpointName;
        int colon = url.indexOf(":");
        if (colon == 1) {
            url = url.substring(2);
        }
        url = "file:" + url;
        props.put(Context.PROVIDER_URL, url);

        RefFSContextFactory fcf = new RefFSContextFactory();
        return (RefFSContext) fcf.getInitialContext(props);
    }
}
