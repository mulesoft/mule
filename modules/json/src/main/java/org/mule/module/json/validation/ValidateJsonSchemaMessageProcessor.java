/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.validation;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link MessageProcessor} that uses a {@link JsonSchemaValidator}
 * to validate a json in the payload to a given schema
 *
 * @since 3.6.0
 */
public class ValidateJsonSchemaMessageProcessor implements MessageProcessor, Initialisable
{

    private String schemaLocation;
    private JsonSchemaDereferencing dereferencing = JsonSchemaDereferencing.CANONICAL;
    private Map<String, String> schemaRedirects = new HashMap<>();

    private JsonSchemaValidator validator;

    @Override
    public void initialise() throws InitialisationException
    {
        validator = JsonSchemaValidator.builder()
                .setSchemaLocation(schemaLocation)
                .setDereferencing(dereferencing)
                .addSchemaRedirects(schemaRedirects)
                .build();
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        validator.validate(event);
        return event;
    }

    public void setSchemaLocation(String schemaLocation)
    {
        this.schemaLocation = schemaLocation;
    }

    public void setDereferencing(JsonSchemaDereferencing dereferencing)
    {
        this.dereferencing = dereferencing;
    }

    public void setSchemaRedirects(Map<String, String> schemaRedirects)
    {
        this.schemaRedirects = schemaRedirects;
    }
}
