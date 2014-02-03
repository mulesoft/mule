/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.event;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;

public class EventTest extends FunctionalTestCase
{
    private Repository repository;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "eventqueue-conf.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        EntryReceiver component = (EntryReceiver)getComponent("eventConsumer");
        component.getReceivedEntries().set(0);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        clearJcrRepository();
        super.doTearDown();
    }

    private void clearJcrRepository()
    {
        try
        {
            if (repository == null)
            {
                return;
            }
            Session session = repository.login(new SimpleCredentials("username", "password".toCharArray()));
    
            Node node = session.getRootNode();
    
            for (NodeIterator itr = node.getNodes(); itr.hasNext();)
            {
                Node child = itr.nextNode();
                if (!child.getName().equals("jcr:system"))
                {
                    child.remove();
                }
            }
            session.save();
            session.logout();
        }
        catch (PathNotFoundException t)
        {
            // do nothing
        }
        catch (Throwable t)
        {
            // do nothing
        }
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testCustomerProvider() throws Exception
    {
        repository = (Repository) muleContext.getRegistry().lookupObject("jcrRepository");

        Thread.sleep(5000);

        AbderaClient client = new AbderaClient();
        ClientResponse res = client.get("http://localhost:" + dynamicPort.getNumber() + "/events");

        Document<Feed> doc = res.getDocument();
        // see if this helps with intermittent failures
        doc.complete();
        Feed feed = doc.getRoot();
        // see if this helps with intermittent failures
        feed.complete();
        assertNotNull("feed is null", feed);
        assertTrue( feed.getEntries().size() >= 1);
        EntryReceiver component = (EntryReceiver)getComponent("eventConsumer");

        assertTrue(component.getCount() > 0);
    }
}
