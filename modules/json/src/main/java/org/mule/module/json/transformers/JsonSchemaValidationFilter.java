/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.transformers;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.xml.filters.SchemaValidationFilter;
import org.mule.util.IOUtils;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Validate a JSON string against an XML schema.
 *
 * Note:
 * Ideally, this would call the Validator using STAX.  Unfortunately,
 * 1. xalan.jar is needed to avoid bugs in the version of Xalan built into the JRE
 * 2. Xalan does not work with STAX
 * 3. Having Xalan in the classpath overrides the default (STAX-compliant) factories for transformations, validators,
 * etc with ones that aren't STAX-compliant
 *
 * The result is that, while the ideal would be to implement this class by validating a STAXSource, that won't be
 * possible until either we can assume a JRE with bith STAX and a working Xalan fork, or there';s a xalan.jar
 * that supports StAX.
 */
public class JsonSchemaValidationFilter extends  SchemaValidationFilter implements MuleContextAware
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
}

