/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.test.mule;

import org.mule.MuleException;
import org.mule.impl.EndpointList;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformers.DefaultTransformer;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>EndpointsListTestCase</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class EndpointsListTestCase extends AbstractMuleTestCase
{
    public void testSendProviderListRegistration() throws Exception
    {
        EndpointList list = new EndpointList();
        list.remove(null);
        //Create send endpoint
        UMOEndpoint test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        test1.setTransformer(new DefaultTransformer());
        list.add(test1);

        assertEquals("There should be 1 endpoint registered", 1, list.getTotalSize());
        assertNotNull("The default send endpoint should be set", list.getDefaultSendProvider());
        assertEquals("Default send endpoint should be test1", "test1", list.getDefaultSendProvider().getName());

        UMOEndpoint test2 = getTestEndpoint("test2", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        list.add(test2);

        assertEquals("There should be 2 endpoints registered", 2, list.getTotalSize());
        assertNotNull("The  send endpoint should be obtainable", list.getSendProvider("test2"));
        assertEquals("Default send endpoint should be test1", "test1", list.getDefaultSendProvider().getName());

        //Override default
        UMOEndpoint test3 = getTestEndpoint("test3", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        list.add(test3);

        assertEquals("There should be 3 endpoints registered", 3, list.getTotalSize());
        assertNotNull("The  send endpoint should be obtainable", list.getSendProvider("test3"));
        assertEquals("Default send endpoint should be test1", "test1", list.getDefaultSendProvider().getName());

        //Now add a sendAndReceive Provider, that will get registered a sender
        UMOEndpoint test4 = getTestEndpoint("test4", UMOEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER);
        list.add(test4);

        assertEquals("There should be 4 endpoints registered", 4, list.getTotalSize());
        assertNotNull("The send endpoint should be obtainable", list.getSendProvider("test4"));
        assertNull("There should be no receive endpoint", list.getReceiveProvider("test4"));


    }

    public void testReceiveProviderListRegistration() throws Exception
    {
        EndpointList list = new EndpointList();
        list.remove(null);
        //Create receive endpoint
        UMOEndpoint test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        test1.setTransformer(new DefaultTransformer());

        list.add(test1);

        assertEquals("There should be 1 endpoint registered", 1, list.getTotalSize());
        assertNotNull("The default Receive endpoint should be set", list.getDefaultReceiveProvider());
        assertEquals("Default Receive endpoint should be test1", "test1", list.getDefaultReceiveProvider().getName());

        UMOEndpoint test2 = getTestEndpoint("test2", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        list.add(test2);

        assertEquals("There should be 2 endpoints registered", list.getTotalSize(), 2);
        assertNotNull("The  Receive endpoint should be obtainable", list.getReceiveProvider("test2"));
        assertEquals("Default Receive endpoint should be test1", "test1", list.getDefaultReceiveProvider().getName());

        //Override default
        UMOEndpoint test3 = getTestEndpoint("test3", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        list.add(test3);

        assertEquals("There should be 3 endpoints registered", 3, list.getTotalSize());
        assertNotNull("The Receive endpoint should be obtainable", list.getReceiveProvider("test3"));
        assertEquals("Default Receive endpoint should be test1", "test1", list.getDefaultReceiveProvider().getName());
    }

    public void testClearProviderList() throws Exception
    {
        EndpointList list = new EndpointList();
        //Create receive endpoint
        UMOEndpoint test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER);

        list.add(test1);

        assertEquals("There should be 1 endpoint registered", 1, list.getTotalSize());

        UMOEndpoint test2 = getTestEndpoint("test2", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        test2.setName("test2");
        list.add(test2);

        assertEquals("There should be 2 endpoints registered", 2, list.getTotalSize());
        assertEquals("There should be 1 receive endpoints registered", 1, list.getReceiveProvidersSize());
        assertEquals("There should be 1 send endpoints registered", 1, list.getSendProvidersSize());
        list.clear();
        assertEquals("There shouldn't be any receive endpoints registered", 0, list.getReceiveProvidersSize());
        assertEquals("There shouldn't be any send endpoints registered", 0, list.getSendProvidersSize());
        assertEquals("There shouldn't be any endpoints registered", 0, list.getTotalSize());

        //Should be able to add items again
        test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        list.add(test1);
        assertEquals("There should be 1 endpoint registered", 1, list.getTotalSize());

    }

    public void testRemoveProvider() throws Exception
    {
        EndpointList list = new EndpointList();
        //This should work
        list.remove(getTestEndpoint("Bla", UMOEndpoint.ENDPOINT_TYPE_SENDER));
        list.remove(null);

        //Create receive endpoint
        UMOEndpoint test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER);
        list.add(test1);

        assertEquals("There should be 1 endpoint registered", 1, list.getTotalSize());

        UMOEndpoint test2 = getTestEndpoint("test2", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        list.add(test2);

        assertEquals("There should be 2 endpoints registered", 2, list.getTotalSize());
        list.remove(test1);

        assertEquals("There shouldn't be any send endpoints registered", 0, list.getSendProvidersSize());
        assertEquals("There should be 1 receive endpoints registered", 1, list.getReceiveProvidersSize());

        list.remove(test2);
        assertEquals("There shouldn't be any endpoints registered", 0, list.getTotalSize());

        //Should be able to add items again
        test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        list.add(test1);
        assertEquals("There should be 1 endpoint registered", 1, list.getTotalSize());

    }

    public void testAccessors() throws Exception
    {
        EndpointList list = new EndpointList();
        assertEquals(list.getTotalSize(), 0);
        UMOEndpoint endpoint = list.getReceiveProvider(null);
        assertNull(endpoint);
        endpoint = list.getSendProvider(null);
        assertNull(endpoint);

        UMOEndpoint test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER);
        list.add(test1);

        UMOEndpoint test2 = getTestEndpoint("test2", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        list.add(test2);

        assertNotNull(list.get("test1"));
        assertNotNull(list.get("test2"));

        assertNull(list.get(null));

    }

    public void testGetByProtocol() throws Exception
    {
        EndpointList list = new EndpointList();
        UMOEndpoint test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        list.add(test1);

        UMOEndpoint test2 = getTestEndpoint("test2", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);

        list.add(test2);


        UMOEndpoint test3 = getTestEndpoint("test3", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        list.add(test3);

        assertEquals(0, list.getReceiveProvidersByProtocol("XXX").length);
        UMOImmutableEndpoint[] descriptors = list.getReceiveProvidersByProtocol(test1.getConnector().getProtocol());
        assertEquals(2, descriptors.length);

        descriptors = list.getSendProvidersByProtocol(test1.getConnector().getProtocol());
        assertEquals(1, descriptors.length);
    }

    public void testSettingDefaultReceiver() throws Exception
    {
        EndpointList list = new EndpointList();
        UMOEndpoint test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        UMOEndpoint test2 = getTestEndpoint("test2", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        UMOEndpoint test3 = getTestEndpoint("test3", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);

        list.add(test1);
        list.add(test2);
        assertEquals(list.getDefaultReceiveProvider().getName(), "test1");

        list.setDefaultReceiveProvider(test3, false);
        assertEquals(list.getDefaultReceiveProvider().getName(), "test3");

        try
        {
            list.setDefaultReceiveProvider(null);
            fail("should throw exception, endpoint cannot be null");
        }
        catch (MuleException e)
        {
            // expected
        }

        //Adding a send endpoint without a transformer should work
        UMOEndpoint test4 = getTestEndpoint("test4", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        list.setDefaultReceiveProvider(test4);
        assertEquals(4, list.getTotalSize());

        assertEquals(list.getDefaultReceiveProvider().getName(), "test4");

        UMOEndpoint test5 = getTestEndpoint("test5", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        test5.setTransformer(new DefaultTransformer());

        try
        {
            list.setDefaultReceiveProvider(test5);
            //I have now lifted this restriction
            //fail("should throw exception, endpoint cannot be sender when it has a transformer");
        }
        catch (MuleException e)
        {
            // expected
            fail("should not throw exception, endpoint can now be sender or receiver when it has a transformer");
        }
    }

    public void testSettingDefaultSender() throws Exception
    {
        EndpointList list = new EndpointList();
        UMOEndpoint test1 = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER);

        UMOEndpoint test2 = getTestEndpoint("test2", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        UMOEndpoint test3 = getTestEndpoint("test3", UMOEndpoint.ENDPOINT_TYPE_SENDER);

        list.add(test1);
        list.add(test2);
        assertEquals(list.getDefaultSendProvider().getName(), "test1");

        list.setDefaultSendProvider(test3, false);
        assertEquals(list.getDefaultSendProvider().getName(), "test3");

        try
        {
            list.setDefaultSendProvider(null);
            //I have now lifted this restriction
            fail("should throw exception, endpoint cannot be null");
        }
        catch (MuleException e)
        {
            // expected

        }

        //Adding a receive endpoint without a transformer should work
        UMOEndpoint test4 = getTestEndpoint("test4", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        list.setDefaultSendProvider(test4);
        assertEquals(4, list.getTotalSize());

        assertEquals(list.getDefaultSendProvider().getName(), "test4");

        UMOEndpoint test5 = getTestEndpoint("test5", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        test5.setTransformer(new DefaultTransformer());
        try
        {
            list.setDefaultSendProvider(test5);
            //I have now lifted this restriction
            //fail("should throw exception, endpoint cannot be receiver when it has a transformer");
        }
        catch (MuleException e)
        {
            // expected
            fail("should not throw exception, endpoint can now be sender or receiver when it has a transformer");
        }
    }

}
