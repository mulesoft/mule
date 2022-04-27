/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.customos.internal.connection;

import static java.util.Optional.of;
import static org.mule.sdk.api.stereotype.MuleStereotypes.CONNECTION_DEFINITION;

import org.mule.sdk.api.stereotype.StereotypeDefinition;

import java.util.Optional;

public class CustomSdkObjectStoreConnectionStereotype implements StereotypeDefinition {

  @Override
  public String getName() {
    return CONNECTION_DEFINITION.getName();
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return of(new StereotypeDefinition() {

      @Override
      public String getName() {
        return CONNECTION_DEFINITION.getName();
      }

      @Override
      public String getNamespace() {
        return "OS";
      }

      @Override
      public Optional<StereotypeDefinition> getParent() {
        return of(CONNECTION_DEFINITION);
      }
    });
  }
}
