/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.jaas;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.EncryptionStrategy;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.security.MuleCredentials;

public abstract class AbstractJaasFunctionalTestCase extends FunctionalTestCase {

  protected static final String TEST_FLOW_NAME = "TestUMO";
  protected static final String TEST_PAYLOAD = "Test";

  protected SecurityHeader createSecurityHeader(String username, String password) throws CryptoFailureException {
    String header = createEncryptedHeader(username, password);
    return new SecurityHeader(MuleProperties.MULE_USER_PROPERTY, header);
  }

  private String createEncryptedHeader(String username, String password) throws CryptoFailureException {
    EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");
    return MuleCredentials.createHeader(username, password, "PBE", strategy);
  }

  protected void runFlowAndExpectUnauthorizedException(SecurityHeader securityHeader) throws Exception {
    MessagingException exception =
        flowRunner(TEST_FLOW_NAME).withInboundProperty(securityHeader.getKey(), securityHeader.getValue())
            .withPayload(TEST_PAYLOAD).runExpectingException();
    assertThat(exception, instanceOf(UnauthorisedException.class));
  }

  public static class SecurityHeader {

    private String key;
    private String value;

    public SecurityHeader(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }
  }
}
