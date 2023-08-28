/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.xmen;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

public class CanadianForest {

  @Parameter
  @Content(primary = true)
  private List<String> bears;

  @Parameter
  @Content
  private TypedValue<Object> river;

  @Parameter
  private List<String> friends;

  public CanadianForest() {
    super();
  }

  public List<String> getBears() {
    return bears;
  }

  public TypedValue<Object> getRiver() {
    return river;
  }

  public List<String> getFriends() {
    return friends;
  }
}
