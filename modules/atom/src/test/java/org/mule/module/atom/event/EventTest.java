/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom.event;

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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EventTest extends FunctionalTestCase
{
    private Repository repository;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
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
