/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.route;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.route.Route;

public class OtherwiseRoute extends Route {

  @Parameter
  @Optional
  private boolean execute;

  public boolean shouldExecute() {
    return execute;
  }

}
