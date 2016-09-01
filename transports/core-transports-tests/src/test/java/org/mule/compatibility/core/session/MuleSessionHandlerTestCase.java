/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_SESSION_PROPERTY;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.security.Authentication;
import org.mule.runtime.core.api.security.Credentials;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.serialization.SerializationException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.message.SessionHandler;
import org.mule.runtime.core.security.DefaultMuleAuthentication;
import org.mule.runtime.core.security.DefaultSecurityContextFactory;
import org.mule.runtime.core.security.MuleCredentials;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.base.Charsets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MuleSessionHandlerTestCase extends AbstractMuleTestCase {

  private static String originalEncoding;

  private MuleContext muleContext;

  @Before
  public void setUp() throws Exception {
    muleContext = mock(MuleContext.class);
    MuleConfiguration configuration = mock(MuleConfiguration.class);
    when(configuration.getDefaultEncoding()).thenReturn(Charsets.UTF_8.name());
    when(muleContext.getConfiguration()).thenReturn(configuration);
    when(muleContext.getExecutionClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
    when(muleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));
    addJavaSerializerToMockMuleContext(muleContext);
  }

  @BeforeClass
  public static void setUpEncoding() {
    originalEncoding = System.getProperty(MULE_ENCODING_SYSTEM_PROPERTY);
    System.setProperty(MULE_ENCODING_SYSTEM_PROPERTY, "UTF-8");
  }

  @AfterClass
  public static void restoreEncoding() {
    if (originalEncoding == null) {
      System.clearProperty(MULE_ENCODING_SYSTEM_PROPERTY);
    } else {
      System.setProperty(MULE_ENCODING_SYSTEM_PROPERTY, originalEncoding);
    }
  }

  /**
   * see EE-1705/MULE-4567
   */
  @Test
  public void testSessionProperties() throws Exception {
    MuleMessage message = MuleMessage.builder().payload("Test Message").build();
    Flow flow = MuleTestUtils.getTestFlow(muleContext);
    MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build();
    SessionHandler handler = new SerializeAndEncodeSessionHandler();

    String string = "bar";
    event.getSession().setProperty("fooString", string);

    Date date = new Date(0);
    event.getSession().setProperty("fooDate", date);

    List<String> list = createList();
    event.getSession().setProperty("fooList", list);

    message = handler.storeSessionInfoToMessage(event.getSession(), event.getMessage(), muleContext);
    event.setMessage(message);
    // store save session to outbound, move it to the inbound
    // for retrieve to deserialize
    Serializable s = removeProperty(event);
    message = MuleMessage.builder(event.getMessage()).addInboundProperty(MULE_SESSION_PROPERTY, s).build();
    MuleSession session = handler.retrieveSessionInfoFromMessage(message, muleContext);

    Object obj = session.getProperty("fooString");
    assertTrue(obj instanceof String);
    assertEquals(string, obj);

    obj = session.getProperty("fooDate");
    assertTrue("Object should be a Date but is " + obj.getClass().getName(), obj instanceof Date);
    assertEquals(date, obj);

    obj = session.getProperty("fooList");
    assertTrue("Object should be a List but is " + obj.getClass().getName(), obj instanceof List);
    assertEquals(list, obj);
  }

  /**
   * see EE-1774
   */
  @Test
  public void testNonSerializableSessionProperties() throws Exception {
    MuleMessage message = MuleMessage.builder().payload("Test Message").build();
    Flow flow = MuleTestUtils.getTestFlow(muleContext);
    MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build();
    SessionHandler handler = new SerializeAndEncodeSessionHandler();

    NotSerializableClass clazz = new NotSerializableClass();
    event.getSession().setProperty("foo", clazz);
    message = handler.storeSessionInfoToMessage(event.getSession(), event.getMessage(), muleContext);
    event.setMessage(message);
    // store save session to outbound, move it to the inbound
    // for retrieve to deserialize
    Serializable s = removeProperty(event);
    message = MuleMessage.builder(event.getMessage()).addInboundProperty(MULE_SESSION_PROPERTY, s).build();
    MuleSession session = handler.retrieveSessionInfoFromMessage(message, muleContext);
    // Property was removed because it could not be serialized
    assertNull(session.getProperty("foo"));
  }

  /**
   * see MULE-4720
   */
  @Test
  public void testSecurityContext() throws Exception {
    MuleMessage message = MuleMessage.builder().payload("Test Message").build();
    Flow flow = MuleTestUtils.getTestFlow(muleContext);
    MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build();
    SessionHandler handler = new SerializeAndEncodeSessionHandler();

    Credentials credentials = new MuleCredentials("joe", "secret".toCharArray());
    SecurityContext sc = new DefaultSecurityContextFactory().create(new DefaultMuleAuthentication(credentials));
    event.getSession().setSecurityContext(sc);

    message = handler.storeSessionInfoToMessage(event.getSession(), event.getMessage(), muleContext);
    event.setMessage(message);
    // store save session to outbound, move it to the inbound
    // for retrieve to deserialize
    Serializable s = removeProperty(event);
    message = MuleMessage.builder(event.getMessage()).addInboundProperty(MULE_SESSION_PROPERTY, s).build();
    MuleSession session = handler.retrieveSessionInfoFromMessage(message, muleContext);

    sc = session.getSecurityContext();
    assertEquals("joe", sc.getAuthentication().getPrincipal());
  }

  private Serializable removeProperty(MuleEvent event) {
    final AtomicReference<Serializable> outbound = new AtomicReference<>();
    outbound.set(event.getMessage().getOutboundProperty(MULE_SESSION_PROPERTY));
    event.setMessage(MuleMessage.builder(event.getMessage()).removeOutboundProperty(MULE_SESSION_PROPERTY).build());

    Object invocation = null;
    try {
      invocation = event.getFlowVariable(MULE_SESSION_PROPERTY);
    } catch (NoSuchElementException nsse) {
      // Ignore
    }
    event.removeFlowVariable(MULE_SESSION_PROPERTY);
    return outbound.get() != null ? outbound.get() : (Serializable) invocation;
  }

  /**
   * see EE-1774
   */
  @Test
  public void testNotSerializableSecurityContext() throws Exception {
    MuleMessage message = MuleMessage.builder().payload("Test Message").build();
    SessionHandler handler = new SerializeAndEncodeSessionHandler();
    MuleSession session = new DefaultMuleSession();

    session.setSecurityContext(new NotSerializableSecurityContext());

    try {
      handler.storeSessionInfoToMessage(session, message, muleContext);
      fail("Should throw a SerializationException");
    } catch (SerializationException e) {
      // expected
    }
  }

  private List<String> createList() {
    List<String> list = new ArrayList<>();
    list.add("bar1");
    list.add("bar2");
    return list;
  }

  private class NotSerializableClass {

    public NotSerializableClass() {
      super();
    }
  }

  private class NotSerializableSecurityContext implements SecurityContext {

    public NotSerializableSecurityContext() {
      super();
    }

    @Override
    public void setAuthentication(Authentication authentication) {
      // nothing to do
    }

    @Override
    public Authentication getAuthentication() {
      return null;
    }
  }

}
