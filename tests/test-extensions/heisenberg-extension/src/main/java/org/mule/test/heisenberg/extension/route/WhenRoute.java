/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.route;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.route.Route;

public class WhenRoute extends Route {

  @Parameter
  @Optional
  private boolean execute;

  @Parameter
  private String message;

  public String getMessage() {
    return message;
  }

  public boolean shouldExecute() {
    return execute;
  }
}
