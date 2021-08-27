/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

/**
 * Class that models the information of a parameter group annotation
 *
 * @since 4.4.0
 */
public class ParameterGroupInfo {

  private String name;
  private boolean showInDsl;

  public ParameterGroupInfo(String name, boolean showInDsl) {
    this.name = name;
    this.showInDsl = showInDsl;
  }

  public String getName() {
    return name;
  }

  public boolean isShowInDsl() {
    return showInDsl;
  }
}
