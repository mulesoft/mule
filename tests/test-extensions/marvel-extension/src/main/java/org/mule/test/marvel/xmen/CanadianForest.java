/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
