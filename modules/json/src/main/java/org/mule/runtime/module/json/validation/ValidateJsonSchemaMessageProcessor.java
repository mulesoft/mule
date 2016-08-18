/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.validation;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link MessageProcessor} that uses a {@link JsonSchemaValidator} to validate a json in the payload to a given schema
 *
 * @since 3.6.0
 */
public class ValidateJsonSchemaMessageProcessor implements MessageProcessor, Initialisable, MuleContextAware {

  private String schemaLocation;
  private JsonSchemaDereferencing dereferencing = JsonSchemaDereferencing.CANONICAL;
  private Map<String, String> schemaRedirects = new HashMap<>();

  private JsonSchemaValidator validator;
  private MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    validator = JsonSchemaValidator.builder().setSchemaLocation(schemaLocation).setDereferencing(dereferencing)
        .addSchemaRedirects(schemaRedirects).build();
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    validator.validate(event, muleContext);
    return event;
  }

  public void setSchemaLocation(String schemaLocation) {
    this.schemaLocation = schemaLocation;
  }

  public void setDereferencing(JsonSchemaDereferencing dereferencing) {
    this.dereferencing = dereferencing;
  }

  public void setSchemaRedirects(Map<String, String> schemaRedirects) {
    this.schemaRedirects = schemaRedirects;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
