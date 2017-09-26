/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
