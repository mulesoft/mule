/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.routing.filter.Filter;
import org.mule.module.json.validation.JsonSchemaValidator;

import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * @deprecated This class is deprecated and will be removed in Mule 4.0. Use {@link JsonSchemaValidator} instead
 */
@Deprecated
public interface JsonSchemaFilter extends Filter, Initialisable, MuleContextAware
{

    Validator createValidator() throws SAXException;

    public String getSchemaLanguage();

    void setSchemaLocations(String schemaLocations);

    String getSchemaLocations();

    public void setSchemaLanguage(String schemaLanguage);

    public Schema getSchemaObject();

    public void setSchemaObject(Schema schemaObject);

    public ErrorHandler getErrorHandler();

    public void setErrorHandler(ErrorHandler errorHandler);

    public LSResourceResolver getResourceResolver();

    public void setResourceResolver(LSResourceResolver resourceResolver);

    public Map<String, Boolean> getValidatorFeatures();

    public void setValidatorFeatures(Map<String, Boolean> validatorFeatures);

    public Map<String, Object> getValidatorProperties();

    public void setValidatorProperties(Map<String, Object> validatorProperties);

    public XMLInputFactory getXMLInputFactory();

    public void setXMLInputFactory(XMLInputFactory xmlInputFactory);

    public boolean isUseStaxSource();

    public void setUseStaxSource(boolean useStaxSource);

    public boolean isReturnResult();

    public void setReturnResult(boolean returnResult);

}
