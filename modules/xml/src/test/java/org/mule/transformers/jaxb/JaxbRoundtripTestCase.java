/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformers.jaxb;

import org.mule.api.transformer.Transformer;
import org.mule.jaxb.model.EmailAddress;
import org.mule.jaxb.model.Person;
import org.mule.module.xml.transformer.jaxb.JAXBMarshallerTransformer;
import org.mule.module.xml.transformer.jaxb.JAXBUnmarshallerTransformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;

import java.util.Arrays;

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

        EmailAddress ea = new EmailAddress("john.doe@gmail.com", "home");
        EmailAddress ea2 = new EmailAddress("jdoe@bigco.com", "work");
        p.setEmailAddresses(Arrays.asList(ea, ea2));

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
