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
 */
package org.mule.jbi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.mule.jbi.framework.ComponentRegistryImpl;
import org.mule.jbi.framework.JbiContainerImpl;
import org.mule.jbi.routing.RouterImpl;
import org.mule.jbi.util.IOUtils;

public class SimpleFunctionalTestCase extends TestCase {

	public static final QName SERVICE_NAME = new QName("http://jbi.mule.org", "myService");
	public static final String ENDPOINT_NAME = "myEndpoint";
	public static final String PAYLOAD = "<payload/>";
	public static final String RESPONSE = "<response/>";
	
	protected JbiContainer container;
	
	public void setUp() throws Exception {
		IOUtils.deleteFile(new File("target/.mule-jbi"));
		JbiContainerImpl jbi = new JbiContainerImpl();
		jbi.setWorkingDir(new File("target/.mule-jbi"));
		List l = MBeanServerFactory.findMBeanServer(null);
		if (l != null && l.size() > 0) {
			jbi.setMBeanServer((MBeanServer) l.get(0));
		} else {
			jbi.setMBeanServer(MBeanServerFactory.createMBeanServer());
		}
		jbi.setRouter(new RouterImpl(jbi));
		jbi.start();
		container = jbi;
	}
	
	public void testInOnly() throws Exception {
		// Create components
		TestComponent provider = new TestComponent();
		TestComponent consumer = new TestComponent();
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("provider", provider);
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("consumer", consumer);
		// Activate endpoint
		ServiceEndpoint endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
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
		// Create components
		TestComponent provider = new TestComponent();
		TestComponent consumer = new TestComponent();
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("provider", provider);
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("consumer", consumer);
		// Activate endpoint
		ServiceEndpoint endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
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
		// Create components
		TestComponent provider = new TestComponent();
		TestComponent consumer = new TestComponent();
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("provider", provider);
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("consumer", consumer);
		// Activate endpoint
		ServiceEndpoint endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
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
		// Create components
		TestComponent provider = new TestComponent();
		TestComponent consumer = new TestComponent();
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("provider", provider);
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("consumer", consumer);
		// Activate endpoint
		ServiceEndpoint endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
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
		// Create components
		TestComponent provider = new TestComponent();
		TestComponent consumer = new TestComponent();
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("provider", provider);
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("consumer", consumer);
		// Activate endpoint
		ServiceEndpoint endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
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
		// Create components
		TestComponent provider = new TestComponent();
		TestComponent consumer = new TestComponent();
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("provider", provider);
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("consumer", consumer);
		// Activate endpoint
		ServiceEndpoint endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
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
		// Create components
		TestComponent provider = new TestComponent();
		TestComponent consumer = new TestComponent();
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("provider", provider);
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("consumer", consumer);
		// Activate endpoint
		ServiceEndpoint endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
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
		// Create components
		TestComponent provider = new TestComponent();
		TestComponent consumer = new TestComponent();
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("provider", provider);
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("consumer", consumer);
		// Activate endpoint
		ServiceEndpoint endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
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
		// Create components
		TestComponent provider = new TestComponent();
		TestComponent consumer = new TestComponent();
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("provider", provider);
		((ComponentRegistryImpl) container.getComponentRegistry()).registerTransientEngineComponent("consumer", consumer);
		// Activate endpoint
		ServiceEndpoint endpoint = provider.getContext().activateEndpoint(SERVICE_NAME, ENDPOINT_NAME);
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
