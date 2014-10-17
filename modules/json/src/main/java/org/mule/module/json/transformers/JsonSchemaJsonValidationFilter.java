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
import org.mule.module.json.validation.ValidateJsonSchemaMessageProcessor;
import org.mule.util.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * @deprecated This class is deprecated and will be removed in Mule 4.0. Use {@link ValidateJsonSchemaMessageProcessor} instead
 */
@Deprecated
public class JsonSchemaJsonValidationFilter implements JsonSchemaFilter
{

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSchemaJsonValidationFilter.class);

    private JsonSchema jsonSchema;
    private String schemaLocations;

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
                LOGGER.warn("Payload type " + input.getClass().getName() + " is not supported");
                return false;
            }

            message.setPayload(output);
            ProcessingReport report = jsonSchema.validate(data);

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("JSON Schema validation report: " + report.toString());
            }

            return report.isSuccess();
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to validate JSON!", e);
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
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
            jsonSchema = schemaFactory.getJsonSchema(jsonNode);
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
