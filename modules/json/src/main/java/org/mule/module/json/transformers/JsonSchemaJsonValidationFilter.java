/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.json.JsonData;
import org.mule.util.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eel.kitchen.jsonschema.main.JsonSchema;
import org.eel.kitchen.jsonschema.main.JsonSchemaFactory;
import org.eel.kitchen.jsonschema.report.ValidationReport;
import org.eel.kitchen.jsonschema.util.JsonLoader;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

public class JsonSchemaJsonValidationFilter implements JsonSchemaFilter
{

    protected transient Log logger = LogFactory.getLog(getClass());

    protected JsonSchema jsonSchema;
    protected String schemaLocations;

    @Override
    public boolean accept(MuleMessage message)
    {
        JsonNode data;
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
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            InputStream inputStream = IOUtils.getResourceAsStream(schemaLocations, getClass());
            InputStreamReader reader = new InputStreamReader(inputStream);
            JsonNode jsonNode = JsonLoader.fromReader(reader);
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.defaultFactory();
            jsonSchema = schemaFactory.fromSchema(jsonNode);
        }
        catch (Exception e)
        {
            Message msg = MessageFactory.createStaticMessage("Unable to load or parse JSON Schema file at: " + schemaLocations);
            throw new InitialisationException(msg, e, this);
        }
    }

    @Override
    public void setSchemaLocations(String schemaLocations)
    {
        this.schemaLocations = schemaLocations;
    }

    @Override
    public String getSchemaLocations()
    {
        return schemaLocations;
    }

    @Override
    public Validator createValidator() throws SAXException
    {
        return null;
    }

    @Override
    public String getSchemaLanguage()
    {
        return null;
    }

    @Override
    public void setSchemaLanguage(String schemaLanguage)
    {
    }

    @Override
    public Schema getSchemaObject()
    {
        return null;
    }

    @Override
    public void setSchemaObject(Schema schemaObject)
    {
    }

    @Override
    public ErrorHandler getErrorHandler()
    {
        return null;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler)
    {
    }

    @Override
    public LSResourceResolver getResourceResolver()
    {
        return null;
    }

    @Override
    public void setResourceResolver(LSResourceResolver resourceResolver)
    {
    }

    @Override
    public Map<String, Boolean> getValidatorFeatures()
    {
        return null;
    }

    @Override
    public void setValidatorFeatures(Map<String, Boolean> validatorFeatures)
    {
    }

    @Override
    public Map<String, Object> getValidatorProperties()
    {
        return null;
    }

    @Override
    public void setValidatorProperties(Map<String, Object> validatorProperties)
    {
    }

    @Override
    public XMLInputFactory getXMLInputFactory()
    {
        return null;
    }

    @Override
    public void setXMLInputFactory(XMLInputFactory xmlInputFactory)
    {
    }

    @Override
    public boolean isUseStaxSource()
    {
        return false;
    }

    @Override
    public void setUseStaxSource(boolean useStaxSource)
    {
    }

    @Override
    public boolean isReturnResult()
    {
        return false;
    }

    @Override
    public void setReturnResult(boolean returnResult)
    {
    }

}
