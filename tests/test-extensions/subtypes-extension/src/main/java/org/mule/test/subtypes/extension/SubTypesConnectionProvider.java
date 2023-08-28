/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;


import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

public class SubTypesConnectionProvider implements ConnectionProvider<SubTypesConnectorConnection> {

  @Parameter
  private ParentShape abstractShape;

  @Parameter
  private Door doorInterface;

  @Parameter
  private List<Door> doors;

  public List<Door> getDoors() {
    return doors;
  }

  @Override
  public SubTypesConnectorConnection connect() throws ConnectionException {
    return new SubTypesConnectorConnection(abstractShape, doorInterface);
  }

  @Override
  public void disconnect(SubTypesConnectorConnection subtypesConnectorConnection) {}

  @Override
  public ConnectionValidationResult validate(SubTypesConnectorConnection subtypesConnectorConnection) {
    return ConnectionValidationResult.success();
  }
}
