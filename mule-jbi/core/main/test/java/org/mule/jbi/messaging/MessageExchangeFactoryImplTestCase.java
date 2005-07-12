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
package org.mule.jbi.messaging;

import java.net.URI;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;

import junit.framework.TestCase;

public class MessageExchangeFactoryImplTestCase extends TestCase {

	protected MessageExchangeFactory mef;
	
	public void setUp() throws Exception {
		this.mef = createMessageExchangeFactory();
	}
	
	protected MessageExchangeFactory createMessageExchangeFactory() {
		return new MessageExchangeFactoryImpl(new DeliveryChannelImpl(null, null));
	}
	
	public void testCreateExchangeWithInvalidPattern() throws Exception {
		try {
			this.mef.createExchange(URI.create("http://fdl"));
			fail("exchange creation should have failed");
		} catch (MessagingException e) {
			// this is ok
		}
	}
	
	public void testCreateInOnlyWithPattern() throws Exception {
		MessageExchange me = this.mef.createExchange(MessageExchangeFactoryImpl.IN_ONLY_PATTERN);
		assertNotNull(me);
		assertTrue(me instanceof InOnly);
	}
	
	public void testCreateInOnly() throws Exception {
		MessageExchange me = this.mef.createInOnlyExchange();
		assertNotNull(me);
		assertTrue(me instanceof InOnly);
	}
	
}
