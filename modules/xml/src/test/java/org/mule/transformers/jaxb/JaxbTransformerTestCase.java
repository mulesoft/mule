/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.jaxb;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.jaxb.model.EmailAddress;
import org.mule.jaxb.model.Person;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.ListDataType;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JaxbTransformerTestCase extends AbstractMuleContextTestCase
{
    public static final String PERSON_XML = "<person><name>John Doe</name><dob>01/01/1970</dob><emailAddresses><emailAddress><type>home</type><address>john.doe@gmail.com</address></emailAddress><emailAddress><type>work</type><address>jdoe@bigco.com</address></emailAddress></emailAddresses></person>";

    @Override
    public void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerObject("trans", new JAXBTestTransformers());
    }

    @Test
    public void testCustomTransform() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(PERSON_XML, muleContext);
        Person person = message.getPayload(DataTypeFactory.create(Person.class));
        assertNotNull(person);
        assertEquals("John Doe", person.getName());
        assertEquals("01/01/1970", person.getDob());
        assertEquals(2, person.getEmailAddresses().size());
        assertEquals("home", person.getEmailAddresses().get(0).getType());
        assertEquals("john.doe@gmail.com", person.getEmailAddresses().get(0).getAddress());
        assertEquals("work", person.getEmailAddresses().get(1).getType());
        assertEquals("jdoe@bigco.com", person.getEmailAddresses().get(1).getAddress());
    }

    @Test
    public void testCustomTransformWithMuleMessage() throws Exception
    {
        ByteArrayInputStream in = new ByteArrayInputStream(PERSON_XML.getBytes());
        DefaultMuleMessage msg = new DefaultMuleMessage(in, muleContext);
        msg.setInboundProperty("foo", "fooValue");
        List<EmailAddress> emailAddresses = msg.getPayload(new ListDataType<List<EmailAddress>>(EmailAddress.class));
        assertNotNull(emailAddresses);
        assertEquals(2, emailAddresses.size());
        assertEquals("home", emailAddresses.get(0).getType());
        assertEquals("john.doe@gmail.com", emailAddresses.get(0).getAddress());
        assertEquals("work", emailAddresses.get(1).getType());
        assertEquals("jdoe@bigco.com", emailAddresses.get(1).getAddress());
    }
}
