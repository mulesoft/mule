/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static java.lang.String.format;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

public class HttpPropertyScopeTestCase extends AbstractPropertyScopeTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/message/http-property-scope.xml";
  }

  protected MuleMessage sendRequest(MuleClient client, MuleMessage message) throws MuleException {
    return client.send(format("http://localhost:%s/foo", port.getNumber()), message, newOptions().method(POST.name()).build())
        .getRight();
  }
}
