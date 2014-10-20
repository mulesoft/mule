/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.json.validation.ValidateJsonSchemaMessageProcessor;
import org.mule.module.xml.filters.SchemaValidationFilter;
import org.mule.util.IOUtils;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Validate a JSON string against an XML schema.
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
 *
 * @deprecated This class is deprecated and will be removed in Mule 4.0. Use {@link ValidateJsonSchemaMessageProcessor} instead
 */
@Deprecated
public class JsonSchemaXsdValidationFilter extends SchemaValidationFilter implements JsonSchemaFilter
{

    protected MuleContext muleContext;
    protected JsonToXml jToX;

    @Override
    public boolean accept(MuleMessage msg)
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

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        jToX = new JsonToXml();
        jToX.setMuleContext(muleContext);
    }

    @Override
    public void setSchemaLocations(String schemaLocations)
    {
        super.setSchemaLocations(schemaLocations);
    }

}
