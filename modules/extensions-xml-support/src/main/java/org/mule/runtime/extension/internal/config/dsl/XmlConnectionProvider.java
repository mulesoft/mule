/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import java.util.Map;

public class XmlConnectionProvider implements ConnectionProvider {

  private Map<String, Object> parameters;

  @Override
  public Object connect() throws ConnectionException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void disconnect(Object connection) {
    // TODO Auto-generated method stub

  }

  @Override
  public ConnectionValidationResult validate(Object connection) {
    // TODO Auto-generated method stub
    return null;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

}
