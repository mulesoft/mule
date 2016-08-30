/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.config.ExceptionHelper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * See MULE-4916: spring beans inside a security filter
 */
public class CustomSecurityFilterTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/security/custom-security-filter-test.xml";
  }

  @Test
  public void testOutboundAutenticationSend() throws Exception {
    Map<String, Serializable> props = new HashMap<>();
    props.put("username", "ross");
    props.put("pass", "ross");

    MuleEvent event = flowRunner("test").withPayload("hi").withInboundProperties(props).run();

    assertNull(event.getError());

    props.put("pass", "badpass");

    MessagingException e = flowRunner("test").withPayload("hi").withInboundProperties(props).runExpectingException();
    assertThat(ExceptionHelper.getRootException(e), instanceOf(BadCredentialsException.class));
  }
}
