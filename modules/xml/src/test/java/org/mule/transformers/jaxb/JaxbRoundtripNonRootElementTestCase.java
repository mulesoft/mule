/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformers.jaxb;

import org.custommonkey.xmlunit.XMLUnit;
import org.mule.api.transformer.Transformer;
import org.mule.jaxb.model.EmailAddress;
import org.mule.jaxb.model.Person;
import org.mule.module.xml.transformer.jaxb.JAXBMarshallerTransformer;
import org.mule.module.xml.transformer.jaxb.JAXBUnmarshallerTransformer;
import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import java.util.ArrayList;
import java.util.List;

public class JaxbRoundtripNonRootElementTestCase extends JaxbRoundtripTestCase
{

    @Override
    public Transformer getTransformer() throws Exception
    {
        JAXBUnmarshallerTransformer t = new JAXBUnmarshallerTransformer(ctx, DataTypeFactory.create(String.class));
        initialiseObject(t);
        return t;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        // Since we're transforming to a non-JAXB type, we can't round-trip
        return null;
    }

    @Override
    public Object getTestData()
    {
        try
        {
            Document doc = XMLUtils.toW3cDocument(super.getTestData());
            return doc.getDocumentElement().getFirstChild();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getResultData()
    {
        return ((Person)super.getResultData()).getName();
    }
}