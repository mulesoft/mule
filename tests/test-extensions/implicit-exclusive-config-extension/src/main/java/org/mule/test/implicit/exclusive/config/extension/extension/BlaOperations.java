/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.implicit.exclusive.config.extension.extension;

import org.mule.runtime.extension.api.annotation.param.Config;

public class BlaOperations {

  public void bla(@Config BlaConfig bla) {}
}
