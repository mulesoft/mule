package org.mule.module.atom.event;

import org.mule.tck.FunctionalTestCase;

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

public class EventTest extends FunctionalTestCase
{

    private Repository repository;

    @Override
    protected String getConfigResources()
    {
        return "eventqueue-conf.xml";
    }

    public void testCustomerProvider() throws Exception
    {
        repository = (Repository) muleContext.getRegistry().lookupObject("jcrRepository");

        Thread.sleep(2000);

        AbderaClient client = new AbderaClient();
        ClientResponse res = client.get("http://localhost:9002/events");

        Document<Feed> doc = res.getDocument();
        Feed feed = doc.getRoot();

        assertEquals(1, feed.getEntries().size());

        assertTrue(EventReceiver.receivedEntries > 0);
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
