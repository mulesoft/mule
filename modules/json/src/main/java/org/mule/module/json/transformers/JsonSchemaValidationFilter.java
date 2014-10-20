/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import static org.mule.module.xml.filters.SchemaValidationFilter.DEFAULT_SCHEMA_LANGUAGE;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.json.validation.ValidateJsonSchemaMessageProcessor;

import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * Validate a JSON string against either an XML schema or Json schema depending on the schema location attribute.
 *
 * @deprecated This class is deprecated and will be removed in Mule 4.0. Use {@link ValidateJsonSchemaMessageProcessor} instead
 */
@Deprecated
public class JsonSchemaValidationFilter implements JsonSchemaFilter
{

    protected JsonSchemaFilter delegate;
    private String schemaLocations;
    private Schema schemaObject;
    private LSResourceResolver resourceResolver;
    private String schemaLanguage = DEFAULT_SCHEMA_LANGUAGE;
    private MuleContext muleContext;
    private ErrorHandler errorHandler;
    private boolean returnResult;
    private boolean useStaxSource;
    private XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    private Map<String, Object> validatorProperties;
    private Map<String, Boolean> validatorFeatures;

    @Override
    public boolean accept(MuleMessage msg)
    {
        return delegate.accept(msg);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        if (delegate != null)
        {
            delegate.setMuleContext(muleContext);
        }
        this.muleContext = muleContext;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (isJsonSchema(schemaLocations))
        {
            delegate = new JsonSchemaJsonValidationFilter();
        }
        else
        {
            delegate = new JsonSchemaXsdValidationFilter();
        }

        delegate.setSchemaLocations(schemaLocations);
        delegate.setSchemaObject(schemaObject);
        delegate.setResourceResolver(resourceResolver);
        delegate.setSchemaLanguage(schemaLanguage);
        delegate.setMuleContext(muleContext);
        delegate.setErrorHandler(errorHandler);
        delegate.setReturnResult(returnResult);
        delegate.setUseStaxSource(useStaxSource);
        delegate.setXMLInputFactory(xmlInputFactory);
        delegate.setValidatorProperties(validatorProperties);
        delegate.setValidatorFeatures(validatorFeatures);

        delegate.initialise();
    }

    protected boolean isJsonSchema(String schema)
    {
        return schema.endsWith(".json");
    }

    @Override
    public void setSchemaLocations(String schemaLocations)
    {
        this.schemaLocations = schemaLocations;
        if (delegate != null)
        {
            delegate.setSchemaLocations(schemaLocations);
        }
    }

    @Override
    public String getSchemaLocations()
    {
        return delegate.getSchemaLocations();
    }

    @Override
    public Validator createValidator() throws SAXException
    {
        return delegate.createValidator();
    }

    @Override
    public String getSchemaLanguage()
    {
        return delegate.getSchemaLanguage();
    }

    @Override
    public Schema getSchemaObject()
    {
        return delegate.getSchemaObject();
    }

    @Override
    public void setSchemaObject(Schema schemaObject)
    {
        this.schemaObject = schemaObject;
        if (delegate != null)
        {
            delegate.setSchemaObject(schemaObject);
        }
    }

    @Override
    public ErrorHandler getErrorHandler()
    {
        return delegate.getErrorHandler();
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
        if (delegate != null)
        {
            delegate.setErrorHandler(errorHandler);
        }
    }

    @Override
    public LSResourceResolver getResourceResolver()
    {
        return delegate.getResourceResolver();
    }

    @Override
    public void setResourceResolver(LSResourceResolver resourceResolver)
    {
        this.resourceResolver = resourceResolver;
        if (delegate != null)
        {
            delegate.setResourceResolver(resourceResolver);
        }
    }

    @Override
    public Map<String, Boolean> getValidatorFeatures()
    {
        return delegate.getValidatorFeatures();
    }

    @Override
    public void setValidatorFeatures(Map<String, Boolean> validatorFeatures)
    {
        this.validatorFeatures = validatorFeatures;
        if (delegate != null)
        {
            delegate.setValidatorFeatures(validatorFeatures);
        }
    }

    @Override
    public Map<String, Object> getValidatorProperties()
    {
        return delegate.getValidatorProperties();
    }

    @Override
    public void setValidatorProperties(Map<String, Object> validatorProperties)
    {
        this.validatorProperties = validatorProperties;
        if (delegate != null)
        {
            delegate.setValidatorProperties(validatorProperties);
        }
    }

    @Override
    public XMLInputFactory getXMLInputFactory()
    {
        return delegate.getXMLInputFactory();
    }

    @Override
    public void setXMLInputFactory(XMLInputFactory xmlInputFactory)
    {
        this.xmlInputFactory = xmlInputFactory;
        if (delegate != null)
        {
            delegate.setXMLInputFactory(xmlInputFactory);
        }
    }

    @Override
    public boolean isUseStaxSource()
    {
        return delegate.isUseStaxSource();
    }

    @Override
    public void setUseStaxSource(boolean useStaxSource)
    {
        this.useStaxSource = useStaxSource;
        if (delegate != null)
        {
            delegate.setUseStaxSource(useStaxSource);
        }
    }

    @Override
    public boolean isReturnResult()
    {
        return delegate.isReturnResult();
    }

    @Override
    public void setReturnResult(boolean returnResult)
    {
        this.returnResult = returnResult;
        if (delegate != null)
        {
            delegate.setReturnResult(returnResult);
        }
    }

    @Override
    public void setSchemaLanguage(String schemaLanguage)
    {
        this.schemaLanguage = schemaLanguage;
        if (delegate != null)
        {
            delegate.setSchemaLanguage(schemaLanguage);
        }
    }
}

