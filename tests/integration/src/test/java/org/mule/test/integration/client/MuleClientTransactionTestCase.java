/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleTransactionConfig;
import org.mule.providers.jms.JmsTransactionFactory;
import org.mule.tck.FunctionalTestCase;
import org.mule.transaction.TransactionCallback;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionTemplate;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class MuleClientTransactionTestCase extends FunctionalTestCase
{

    protected String getConfigResources() {
        return "org/mule/test/integration/client/test-client-jms-mule-config.xml";
    }
    
    public void testTransactionsWithSetRollbackOnly() throws Exception 
    {
        final MuleClient client = new MuleClient();
        final Map props = new HashMap();
        props.put("JMSReplyTo", "replyTo.queue");
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");
        
        // Empty reply queue
        while (client.receive("jms://replyTo.queue", 2000) != null) {
            // slurp
        }
        
        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate tt = new TransactionTemplate(tc, null);
        tt.execute(new TransactionCallback() {
            public Object doInTransaction() throws Exception
            {
                for (int i = 0; i < 100; i++) {
                    client.send("jms://test.queue", "Test Client Dispatch message " + i, props);
                }
                UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
                assertNotNull(tx);
                tx.setRollbackOnly();
                return null;
            }
        });
        
        UMOMessage result = client.receive("jms://replyTo.queue", 2000);
        assertNull(result);
    }

    public void testTransactionsWithExceptionThrown() throws Exception 
    {
        final MuleClient client = new MuleClient();
        final Map props = new HashMap();
        props.put("JMSReplyTo", "replyTo.queue");
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");
        
        // Empty reply queue
        while (client.receive("jms://replyTo.queue", 2000) != null) {
            // hmm..mesages
        }

        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate tt = new TransactionTemplate(tc, null);
        try {
            tt.execute(new TransactionCallback() {
                public Object doInTransaction() throws Exception
                {
                    for (int i = 0; i < 100; i++) {
                        client.send("jms://test.queue", "Test Client Dispatch message " + i, props);
                    }
                    throw new Exception();
                }
            });
            fail();
        } catch (Exception e) {
            // this is ok
        }
        
        UMOMessage result = client.receive("jms://replyTo.queue", 2000);
        assertNull(result);
    }

    public void testTransactionsWithCommit() throws Exception 
    {
        final MuleClient client = new MuleClient();
        final Map props = new HashMap();
        props.put("JMSReplyTo", "replyTo.queue");
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");
        
        // Empty reply queue
        while (client.receive("jms://replyTo.queue", 2000) != null) {
            // yum!
        }

        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate tt = new TransactionTemplate(tc, null);
        tt.execute(new TransactionCallback() {
            public Object doInTransaction() throws Exception 
            {
                for (int i = 0; i < 100; i++) {
                    client.send("jms://test.queue", "Test Client Dispatch message " + i, props);
                }
                return null;
            }
        });

        for (int i = 0; i < 100; i++) {
            UMOMessage result = client.receive("jms://replyTo.queue", 2000);
            assertNotNull(result);
        }
        UMOMessage result = client.receive("jms://replyTo.queue", 2000);
        assertNull(result);
    }
    
    protected void emptyReplyQueue() throws Exception {
        final MuleClient client = new MuleClient();
        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate tt = new TransactionTemplate(tc, null);
        tt.execute(new TransactionCallback() {
            public Object doInTransaction() throws Exception 
            {
                while (client.receive("jms://replyTo.queue", 2000) != null) {
                    // munch..
                }

                return null;
            }
        });
    }

}
