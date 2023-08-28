/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.xmen;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class MutantUnitedResponse {


  @Parameter
  @ParameterDsl(allowReferences = false)
  @Content(primary = true)
  @Summary("The body of the Message")
  private TypedValue<Object> body;

  public TypedValue<Object> getBody() {
    return body;
  }

}
