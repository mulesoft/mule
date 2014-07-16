/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.filters;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.transformer.DelayedResult;
import org.mule.module.xml.util.MuleResourceResolver;
import org.mule.module.xml.util.XMLUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * Filter for schema validation.
 * 
 **/
public class SchemaValidationFilter extends AbstractJaxpFilter implements Filter, Initialisable
{
    public static final String DEFAULT_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";

    protected transient Log logger = LogFactory.getLog(getClass());
    private String schemaLocations;
    private String schemaLanguage = DEFAULT_SCHEMA_LANGUAGE;
    private Schema schemaObject;
    private ErrorHandler errorHandler;
    private Map<String, Boolean> validatorFeatures;
    private Map<String, Object> validatorProperties;
    private LSResourceResolver resourceResolver;
    private boolean useStaxSource = false;
    private boolean returnResult = true;
    private XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    /**
     * Accepts the message if schema validation passes.
     * 
     * @param message The message.
     * @return Whether the message passes schema validation.
     */
    public boolean accept(MuleMessage message)
    {
        Source source;
        try
        {
            source = loadSource(message);
        }
        catch (Exception e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            
            if (logger.isInfoEnabled())
            {
                logger.info("SchemaValidationFilter rejected a message because there was a problem interpreting the payload as XML.", e);
            }
            return false;
        }

        if (source == null)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("SchemaValidationFilter rejected a message because the XML source was null.");
            }
            return false;
        }

        
        DOMResult result = null;
        
        try
        {
            if (returnResult) 
            {
                result = new DOMResult();
                createValidator().validate(source, result);
            }
            else 
            {
                createValidator().validate(source);
            }
        }
        catch (SAXException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                    "SchemaValidationFilter rejected a message because it apparently failed to validate against the schema.",
                    e);
            }
            return false;
        }
        catch (IOException e)
        {
            if (logger.isInfoEnabled())
            {
                logger.info(
                    "SchemaValidationFilter rejected a message because there was a problem reading the XML.",
                    e);
            }
            return false;
        }
        finally 
        {
            if (result != null && result.getNode() != null)
            {
                message.setPayload(result.getNode());
            }
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("SchemaValidationFilter accepted the message.");
        }

        return true;
    }

    /**
     * Get a delayed result.
     * 
     * @param source The source.
     * @return The result.
     */
    protected Object getDelayedResult(final Source source)
    {
        return new DelayedResult()
        {
            private String systemId;

            public void write(Result result) throws Exception
            {
                createValidator().validate(source, result);
            }

            public String getSystemId()
            {
                return systemId;
            }

            public void setSystemId(String systemId)
            {
                this.systemId = systemId;
            }
        };
    }

    /**
     * Load the source from the specified object.
     * 
     * @param msg Encompassing message
     * @return The source
     */
    protected Source loadSource(MuleMessage msg) throws Exception
    {
        Object payload = msg.getPayload();
        if (returnResult)
        {
            // Validation requires that a DOM goes in for a DOM to go out
            payload = toDOMNode(payload);
        }
        return XMLUtils.toXmlSource(getXMLInputFactory(), isUseStaxSource(), payload);
    }

    public void initialise() throws InitialisationException
    {
        super.initialise();
        
        if (getSchemaObject() == null)
        {
            if (schemaLocations == null)
            {
                throw new InitialisationException(CoreMessages.objectIsNull("schemaLocations"), this);
            }

            String[] split = StringUtils.splitAndTrim(schemaLocations, ",");
            Source[] schemas = new Source[split.length];
            for (int i = 0; i < split.length; i++)
            {
                String loc = split[i];
                InputStream schemaStream;
                try
                {
                    schemaStream = loadSchemaStream(loc);
                }
                catch (IOException e)
                {
                    throw new InitialisationException(e, this);
                }
    
                if (schemaStream == null)
                {
                    throw new InitialisationException(CoreMessages.failedToLoad(loc), this);
                }
                
                schemas[i] = new StreamSource(schemaStream);
            }
            
            SchemaFactory schemaFactory = SchemaFactory.newInstance(getSchemaLanguage());

            if (logger.isInfoEnabled())
            {
                logger.info("Schema factory implementation: " + schemaFactory);
            }

            if (this.errorHandler != null)
            {
                schemaFactory.setErrorHandler(this.errorHandler);
            }

            if (this.resourceResolver == null)
            {
                this.resourceResolver = new MuleResourceResolver();
            }

            schemaFactory.setResourceResolver(this.resourceResolver);

            Schema schema;
            try
            {
                schema = schemaFactory.newSchema(schemas);
            }
            catch (SAXException e)
            {
                throw new InitialisationException(e, this);
            }

            setSchemaObject(schema);
        }

        if (getSchemaObject() == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("schemaObject"), this);
        }
    }

    protected InputStream loadSchemaStream(String schemaLocation) throws IOException
    {
        return IOUtils.getResourceAsStream(schemaLocation, getClass());
    }

    /**
     * Create a validator.
     * 
     * @return The validator.
     */
    public Validator createValidator() throws SAXException
    {
        Validator validator = getSchemaObject().newValidator();

        if (this.validatorFeatures != null)
        {
            for (Map.Entry<String, Boolean> feature : this.validatorFeatures.entrySet())
            {
                validator.setFeature(feature.getKey(), feature.getValue());
            }
        }

        if (this.validatorProperties != null)
        {
            for (Map.Entry<String, Object> validatorProperty : this.validatorProperties.entrySet())
            {
                validator.setProperty(validatorProperty.getKey(), validatorProperty.getValue());
            }
        }

        return validator;
    }

    public String getSchemaLocations()
    {
        return schemaLocations;
    }

    public void setSchemaLocations(String schemaLocations)
    {
        this.schemaLocations = schemaLocations;
    }

    public String getSchemaLanguage()
    {
        return schemaLanguage;
    }

    public void setSchemaLanguage(String schemaLanguage)
    {
        this.schemaLanguage = schemaLanguage;
    }

    public Schema getSchemaObject()
    {
        return schemaObject;
    }

    public void setSchemaObject(Schema schemaObject)
    {
        this.schemaObject = schemaObject;
    }

    public ErrorHandler getErrorHandler()
    {
        return errorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
    }

    public LSResourceResolver getResourceResolver()
    {
        return resourceResolver;
    }

    public void setResourceResolver(LSResourceResolver resourceResolver)
    {
        this.resourceResolver = resourceResolver;
    }

    public Map<String, Boolean> getValidatorFeatures()
    {
        return validatorFeatures;
    }

    public void setValidatorFeatures(Map<String, Boolean> validatorFeatures)
    {
        this.validatorFeatures = validatorFeatures;
    }

    public Map<String, Object> getValidatorProperties()
    {
        return validatorProperties;
    }

    public void setValidatorProperties(Map<String, Object> validatorProperties)
    {
        this.validatorProperties = validatorProperties;
    }

    public XMLInputFactory getXMLInputFactory()
    {
        return xmlInputFactory;
    }

    public void setXMLInputFactory(XMLInputFactory xmlInputFactory)
    {
        this.xmlInputFactory = xmlInputFactory;
    }

    public boolean isUseStaxSource()
    {
        return useStaxSource;
    }

    public void setUseStaxSource(boolean useStaxSource)
    {
        this.useStaxSource = useStaxSource;
    }

    public boolean isReturnResult()
    {
        return returnResult;
    }

    public void setReturnResult(boolean returnResult)
    {
        this.returnResult = returnResult;
    }
}
