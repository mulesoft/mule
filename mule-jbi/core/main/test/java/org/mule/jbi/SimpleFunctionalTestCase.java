/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.jbi;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.mule.jbi.framework.JbiContainerImpl;

public class SimpleFunctionalTestCase extends TestCase {

	public static final QName SERVICE_NAME = new QName("http://jbi.mule.org", "myService");
	public static final String ENDPOINT_NAME = "myEndpoint";
	public static final String PAYLOAD = "<payload/>";
	public static final String RESPONSE = "<response/>";
	
	private JbiContainer container;
	private TestComponent provider;
	private TestComponent consumer;
	private ServiceEndpoint endpoint;
	
	public void setUp() throws Exception {
		// Remove jbi workspace
		//IOUtils.deleteFile(new File("target/.mule-jbi"));
		// Create jbi container
		JbiContainerImpl jbi = new JbiContainerImpl();
		jbi.setWorkingDir(new File("target/.mule-jbi"));
		container = jbi;
		// Initialize jbi
		container.initialize();
		// Create components
		provider = new TestComponent();
		consumer = new TestComponent();
		// Register components
		container.getRegistry().addTransientEngine("provider", provider);
		container.getRegistry().addTransientEngine("consumer", consumer);
		// Start jbi
		container.start();
		// Activate endpoint
		endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
	}
	
	public void tearDown() throws Exception {
		if (container != null) {
			container.shutDown();
		}
		container = null;
	}
	
	public void testInOnly() throws Exception {
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory(endpoint);
		InOnly mec = mef.createInOnlyExchange();
		NormalizedMessage m = mec.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(PAYLOAD.getBytes())));
		mec.setInMessage(m);
		consumer.getChannel().send(mec);
		// Provider side
		InOnly mep = (InOnly) provider.getChannel().accept(10L);
		assertNotNull(mep);
		assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
		mep.setStatus(ExchangeStatus.DONE);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.DONE, mec.getStatus());
		// Nothing left
		assertNull(consumer.getChannel().accept(10L)); // receive in
		assertNull(provider.getChannel().accept(10L)); // receive in
	}
	
	public void testInOut() throws Exception {
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory(endpoint);
		InOut mec = mef.createInOutExchange();
		NormalizedMessage m = mec.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(PAYLOAD.getBytes())));
		mec.setInMessage(m);
		consumer.getChannel().send(mec);
		// Provider side
		InOut mep = (InOut) provider.getChannel().accept(10L);
		assertNotNull(mep);
		assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
		m = mep.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(RESPONSE.getBytes())));
		mep.setOutMessage(m);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
		mec.setStatus(ExchangeStatus.DONE);
		consumer.getChannel().send(mec);
		// Provider site
		assertSame(mep, provider.getChannel().accept(10L));
		assertEquals(ExchangeStatus.DONE, mec.getStatus());
		// Nothing left
		assertNull(consumer.getChannel().accept(10L)); // receive in
		assertNull(provider.getChannel().accept(10L)); // receive in
	}
	
	public void testInOutWithFault() throws Exception {
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory(endpoint);
		InOut mec = mef.createInOutExchange();
		NormalizedMessage m = mec.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(PAYLOAD.getBytes())));
		mec.setInMessage(m);
		consumer.getChannel().send(mec);
		// Provider side
		InOut mep = (InOut) provider.getChannel().accept(10L);
		assertNotNull(mep);
		assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
		m = mep.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(RESPONSE.getBytes())));
		mep.setStatus(ExchangeStatus.ERROR);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.ERROR, mec.getStatus());
		mec.setStatus(ExchangeStatus.DONE);
		consumer.getChannel().send(mec);
		// Provider site
		assertSame(mep, provider.getChannel().accept(10L));
		assertEquals(ExchangeStatus.DONE, mec.getStatus());
		// Nothing left
		assertNull(consumer.getChannel().accept(10L)); // receive in
		assertNull(provider.getChannel().accept(10L)); // receive in
	}
	
	public void testInOptOutWithRep() throws Exception {
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory(endpoint);
		InOptionalOut mec = mef.createInOptionalOutExchange();
		NormalizedMessage m = mec.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(PAYLOAD.getBytes())));
		mec.setInMessage(m);
		consumer.getChannel().send(mec);
		// Provider side
		InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(10L);
		assertNotNull(mep);
		assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
		m = mep.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(RESPONSE.getBytes())));
		mep.setOutMessage(m);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
		mec.setStatus(ExchangeStatus.DONE);
		consumer.getChannel().send(mec);
		// Provider site
		assertSame(mep, provider.getChannel().accept(10L));
		assertEquals(ExchangeStatus.DONE, mec.getStatus());
		// Nothing left
		assertNull(consumer.getChannel().accept(10L)); // receive in
		assertNull(provider.getChannel().accept(10L)); // receive in
	}
	
	public void testInOptOutWithoutRep() throws Exception {
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory(endpoint);
		InOptionalOut mec = mef.createInOptionalOutExchange();
		NormalizedMessage m = mec.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(PAYLOAD.getBytes())));
		mec.setInMessage(m);
		consumer.getChannel().send(mec);
		// Provider side
		InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(10L);
		assertNotNull(mep);
		assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
		mep.setStatus(ExchangeStatus.DONE);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.DONE, mec.getStatus());
		// Nothing left
		assertNull(consumer.getChannel().accept(10L)); // receive in
		assertNull(provider.getChannel().accept(10L)); // receive in
	}
	
	public void testInOptOutWithProviderFault() throws Exception {
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory(endpoint);
		InOptionalOut mec = mef.createInOptionalOutExchange();
		NormalizedMessage m = mec.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(PAYLOAD.getBytes())));
		mec.setInMessage(m);
		consumer.getChannel().send(mec);
		// Provider side
		InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(10L);
		assertNotNull(mep);
		assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
		mep.setStatus(ExchangeStatus.ERROR);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.ERROR, mec.getStatus());
		mec.setStatus(ExchangeStatus.DONE);
		consumer.getChannel().send(mec);
		// Provider site
		assertSame(mep, provider.getChannel().accept(10L));
		assertEquals(ExchangeStatus.DONE, mec.getStatus());
		// Nothing left
		assertNull(consumer.getChannel().accept(10L)); // receive in
		assertNull(provider.getChannel().accept(10L)); // receive in
	}
	
	public void testInOptOutWithRepAndConsumerFault() throws Exception {
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory(endpoint);
		InOptionalOut mec = mef.createInOptionalOutExchange();
		NormalizedMessage m = mec.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(PAYLOAD.getBytes())));
		mec.setInMessage(m);
		consumer.getChannel().send(mec);
		// Provider side
		InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(10L);
		assertNotNull(mep);
		assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
		m = mep.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(RESPONSE.getBytes())));
		mep.setOutMessage(m);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
		mec.setStatus(ExchangeStatus.ERROR);
		consumer.getChannel().send(mec);
		// Provider site
		assertSame(mep, provider.getChannel().accept(10L));
		assertEquals(ExchangeStatus.ERROR, mep.getStatus());
		mep.setStatus(ExchangeStatus.DONE);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.DONE, mec.getStatus());
		// Nothing left
		assertNull(consumer.getChannel().accept(10L)); // receive in
		assertNull(provider.getChannel().accept(10L)); // receive in
	}
	
	public void testRobustInOnly() throws Exception {
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory(endpoint);
		RobustInOnly mec = mef.createRobustInOnlyExchange();
		NormalizedMessage m = mec.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(PAYLOAD.getBytes())));
		mec.setInMessage(m);
		consumer.getChannel().send(mec);
		// Provider side
		RobustInOnly mep = (RobustInOnly) provider.getChannel().accept(10L);
		assertNotNull(mep);
		assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
		mep.setStatus(ExchangeStatus.DONE);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.DONE, mec.getStatus());
		// Nothing left
		assertNull(consumer.getChannel().accept(10L)); // receive in
		assertNull(provider.getChannel().accept(10L)); // receive in
	}
	
	public void testRobustInOnlyWithFault() throws Exception {
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory(endpoint);
		RobustInOnly mec = mef.createRobustInOnlyExchange();
		NormalizedMessage m = mec.createMessage();
		m.setContent(new StreamSource(new ByteArrayInputStream(PAYLOAD.getBytes())));
		mec.setInMessage(m);
		consumer.getChannel().send(mec);
		// Provider side
		RobustInOnly mep = (RobustInOnly) provider.getChannel().accept(10L);
		assertNotNull(mep);
		assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
		mep.setStatus(ExchangeStatus.ERROR);
		provider.getChannel().send(mep);
		// Consumer side
		assertSame(mec, consumer.getChannel().accept(10L));
		assertEquals(ExchangeStatus.ERROR, mec.getStatus());
		mec.setStatus(ExchangeStatus.DONE);
		provider.getChannel().send(mec);
		// Provider site
		assertSame(mep, provider.getChannel().accept(10L));
		assertEquals(ExchangeStatus.DONE, mep.getStatus());
		// Nothing left
		assertNull(consumer.getChannel().accept(10L)); // receive in
		assertNull(provider.getChannel().accept(10L)); // receive in
	}
	
}
