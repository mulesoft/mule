/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.event;

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;

public class AtomEventTest extends FunctionalTestCase
{
    private Repository repository;

    @Override
    protected String getConfigResources()
    {
        return "atom-builder-conf.xml";
    }

    public void testCustomerProvider() throws Exception
    {
        repository = (Repository) muleContext.getRegistry().lookupObject("jcrRepository");

        MuleClient client = new MuleClient();

        Map props = new HashMap();
        props.put("title", "Foo Bar");

        client.send("vm://in", "Mmm feeding", props);

        Thread.sleep(1000);

        AbderaClient aClient = new AbderaClient();
        ClientResponse res = aClient.get("http://localhost:9002/events");

        Document<Feed> doc = res.getDocument();
        Feed feed = doc.getRoot();

        assertEquals(1, feed.getEntries().size());
        Entry e = feed.getEntries().get(0);

        assertEquals("Mmm feeding", e.getContent());
        assertEquals("Ross Mason", e.getAuthor().getName());
        assertEquals("Foo Bar", e.getTitle());

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
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

}