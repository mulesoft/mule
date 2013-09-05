/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.json.JsonData;
import org.mule.module.xml.filters.SchemaValidationFilter;
import org.mule.util.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.eel.kitchen.jsonschema.main.JsonSchema;
import org.eel.kitchen.jsonschema.main.JsonSchemaFactory;
import org.eel.kitchen.jsonschema.report.ValidationReport;
import org.eel.kitchen.jsonschema.util.JsonLoader;

/**
 * Validate a JSON string against either an XML schema or Json schema depending on the schema location attribute.
 * <p/>
 * Note:
 * Ideally, this would call the Validator using STAX.  Unfortunately,
 * 1. xalan.jar is needed to avoid bugs in the version of Xalan built into the JRE
 * 2. Xalan does not work with STAX
 * 3. Having Xalan in the classpath overrides the default (STAX-compliant) factories for transformations, validators,
 * etc with ones that aren't STAX-compliant
 * <p/>
 * The result is that, while the ideal would be to implement this class by validating a STAXSource, that won't be
 * possible until either we can assume a JRE with bith STAX and a working Xalan fork, or there';s a xalan.jar
 * that supports StAX.
 */
public class JsonSchemaValidationFilter extends SchemaValidationFilter implements MuleContextAware
{

    protected MuleContext muleContext;
    protected JsonToXml jToX;
    protected JsonSchema jsonSchema;

    @Override
    public boolean accept(MuleMessage msg)
    {
        if (isJsonSchema())
        {
            return acceptJsonSchema(msg);
        }
        return acceptXmlSchema(msg);

    }

    protected boolean acceptXmlSchema(MuleMessage msg)
    {
        String jsonString = null;

        try
        {
            if (isReturnResult())
            {
                TransformerInputs transformerInputs = new TransformerInputs(null, msg.getPayload());
                Writer jsonWriter = new StringWriter();
                if (transformerInputs.getInputStream() != null)
                {
                    jsonWriter = new StringWriter();
                    IOUtils.copy(transformerInputs.getInputStream(), jsonWriter, msg.getEncoding());
                }
                else
                {
                    IOUtils.copy(transformerInputs.getReader(), jsonWriter);
                }
                jsonString = jsonWriter.toString();
                msg.setPayload(jsonString);
            }
            String xmlString = (String) jToX.transform(msg.getPayload(), msg.getEncoding());
            MuleMessage xmlMessage = new DefaultMuleMessage(xmlString, msg, msg.getMuleContext());
            boolean accepted = super.accept(xmlMessage);
            if (jsonString != null)
            {
                msg.setPayload(jsonString);
            }
            return accepted;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    protected boolean acceptJsonSchema(MuleMessage message)
    {
        JsonNode data = null;
        Object input = message.getPayload();
        Object output = input;

        try
        {
            if (input instanceof String)
            {
                data = JsonLoader.fromString((String) input);
            }
            else if (input instanceof Reader)
            {
                data = JsonLoader.fromReader((Reader) input);
                output = data.toString();
            }
            else if (input instanceof InputStream)
            {
                data = JsonLoader.fromReader(new InputStreamReader((InputStream) input));
                output = data.toString();
            }
            else if (input instanceof byte[])
            {
                data = JsonLoader.fromReader(new InputStreamReader(new ByteArrayInputStream((byte[]) input)));
            }
            else if (input instanceof JsonNode)
            {
                data = (JsonNode) input;
            }
            else if (input instanceof JsonData)
            {
                JsonData jsonData = (JsonData) input;
                data = JsonLoader.fromReader(new StringReader(jsonData.toString()));
            }
            else
            {
                logger.warn("Payload type " + input.getClass().getName() + " is not supported");
                return false;
            }
            message.setPayload(output);
            ValidationReport report = jsonSchema.validate(data);
            logger.debug("JSON Schema validation report: " + report.getMessages());
            return report.isSuccess();
        }
        catch (Exception e)
        {
            logger.info("Unable to validate JSON!", e);
            return false;
        }
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (isJsonSchema())
        {
            doInitialiseJsonSchema();
        }
        else
        {
            super.initialise();
            jToX = new JsonToXml();
            jToX.setMuleContext(muleContext);
        }
    }

    protected void doInitialiseJsonSchema() throws InitialisationException
    {
        try
        {
            InputStream inputStream = IOUtils.getResourceAsStream(getSchemaLocations(), getClass());
            InputStreamReader reader = new InputStreamReader(inputStream);
            JsonNode jsonNode = JsonLoader.fromReader(reader);
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.defaultFactory();
            jsonSchema = schemaFactory.fromSchema(jsonNode);
        }
        catch (Exception e)
        {
            Message msg = MessageFactory.createStaticMessage("Unable to load or parse JSON Schema file at: " + getSchemaLocations());
            throw new InitialisationException(msg, e, this);
        }
    }

    @Override
    public void setSchemaLocations(String schemaLocations)
    {
        super.setSchemaLocations(schemaLocations);
    }

    protected boolean isJsonSchema()
    {
        return getSchemaLocations().endsWith(".json");
    }
}

