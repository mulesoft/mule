/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

/**
 * Class that models information of a parameter group
 *
 * @since 4.5.0
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
