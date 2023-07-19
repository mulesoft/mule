/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension.stereotype;

import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONNECTION_DEFINITION;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

import java.util.Optional;

public class ZooConnectionStereotype implements StereotypeDefinition {

  @Override
  public String getName() {
    return "ZOO_CONNECTION";
  }

  @Override
  public String getNamespace() {
    return "ZOO";
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return Optional.of(CONNECTION_DEFINITION);
  }
}
