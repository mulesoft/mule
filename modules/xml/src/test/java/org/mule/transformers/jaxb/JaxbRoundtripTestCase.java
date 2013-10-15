/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.jaxb;

import org.mule.api.transformer.Transformer;
import org.mule.jaxb.model.EmailAddress;
import org.mule.jaxb.model.Person;
import org.mule.module.xml.transformer.jaxb.JAXBMarshallerTransformer;
import org.mule.module.xml.transformer.jaxb.JAXBUnmarshallerTransformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;

import org.custommonkey.xmlunit.XMLUnit;

import static org.junit.Assert.fail;

public class JaxbRoundtripTestCase extends AbstractTransformerTestCase
{
    protected JAXBContext ctx;

    @Override
    protected void doSetUp() throws Exception
    {
        ctx = JAXBContext.newInstance(Person.class);
        super.doSetUp();
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        JAXBUnmarshallerTransformer t = new JAXBUnmarshallerTransformer(ctx, DataTypeFactory.create(Person.class));
        initialiseObject(t);
        return t;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        JAXBMarshallerTransformer t = new JAXBMarshallerTransformer(ctx, DataTypeFactory.STRING);
        initialiseObject(t);
        return t;
    }

    @Override
    public Object getTestData()
    {
        return "<person><name>John Doe</name><dob>01/01/1970</dob><emailAddresses><emailAddress><type>home</type><address>john.doe@gmail.com</address></emailAddress><emailAddress><type>work</type><address>jdoe@bigco.com</address></emailAddress></emailAddresses></person>";
    }

    @Override
    public Object getResultData()
    {
        Person p = new Person();
        p.setName("John Doe");
        p.setDob("01/01/1970");
        List<EmailAddress> emailAddresses = new ArrayList<EmailAddress>(1);
        EmailAddress ea = new EmailAddress();
        ea.setType("home");
        ea.setAddress("john.doe@gmail.com");
        emailAddresses.add(ea);
        EmailAddress ea2 = new EmailAddress();
        ea2.setType("work");
        ea2.setAddress("jdoe@bigco.com");
        emailAddresses.add(ea2);
        p.setEmailAddresses(emailAddresses);
        return p;
    }

    @Override
    public boolean compareRoundtripResults(Object expected, Object result)
    {
        try
        {
            return XMLUnit.compareXML(expected.toString(), result.toString()).similar();
        }
        catch (Exception e)
        {
            fail("Failed to compare roudtrip XML strings: " + e.getMessage());
            return false;
        }
    }
}
