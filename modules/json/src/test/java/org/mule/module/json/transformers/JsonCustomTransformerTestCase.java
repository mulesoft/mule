/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.transformer.AnnotatedTransformerProxy;
import org.mule.json.model.EmailAddress;
import org.mule.json.model.Item;
import org.mule.json.model.Person;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.CollectionDataType;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.ListDataType;
import org.mule.transformer.types.SimpleDataType;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class JsonCustomTransformerTestCase extends AbstractMuleContextTestCase
{
    public static final String PERSON_JSON = "{\"emailAddresses\":[{\"type\":\"home\",\"address\":\"john.doe@gmail.com\"},{\"type\":\"work\",\"address\":\"jdoe@bigco.com\"}],\"name\":\"John Doe\",\"dob\":\"01/01/1970\"}";
    public static final String EMAIL_JSON = "{\"type\":\"home\",\"address\":\"john.doe@gmail.com\"}";
    public static final String ITEMS_JSON = "[{\"code\":\"1234\",\"description\":\"Vacuum Cleaner\",\"in-stock\":true},{\"code\":\"1234-1\",\"description\":\"Cleaner Bag\",\"in-stock\":false}]";

    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerObject("trans", new JsonCustomTransformer());
    }

    @Test
    public void testCustomTransform() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(PERSON_JSON, muleContext);

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
        ByteArrayInputStream in = new ByteArrayInputStream(EMAIL_JSON.getBytes());
        DefaultMuleMessage message = new DefaultMuleMessage(in, muleContext);
        message.setInboundProperty("foo", "fooValue");
        EmailAddress emailAddress = message.getPayload(new SimpleDataType<EmailAddress>(EmailAddress.class));
        assertNotNull(emailAddress);
        assertEquals("home", emailAddress.getType());
        assertEquals("john.doe@gmail.com", emailAddress.getAddress());
    }

    @Test
    public void testCustomListTransform() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(ITEMS_JSON, muleContext);
        List<Item> items = message.getPayload(new CollectionDataType<List<Item>>(List.class, Item.class));
        assertNotNull(items);
        assertEquals("1234", items.get(0).getCode());
        assertEquals("Vacuum Cleaner", items.get(0).getDescription());
        assertEquals("1234-1", items.get(1).getCode());
        assertEquals("Cleaner Bag", items.get(1).getDescription());

        //Call this transformer here to test that the cached transformer from the previous invocation does not interfer with
        //Finding the List<Person> transformer
        String people_json = "[" + PERSON_JSON + "," + PERSON_JSON + "]";
        message = new DefaultMuleMessage(people_json, muleContext);

        List<Person> people = message.getPayload(new CollectionDataType<List<Person>>(List.class, Person.class));
        assertNotNull(people);
        assertEquals(2, people.size());
    }

    @Test
    public void testDifferentListTransformer() throws Exception
    {
        //Test that we can resolve other collections

        String people_json = "[" + PERSON_JSON + "," + PERSON_JSON + "]";
        MuleMessage message = new DefaultMuleMessage(people_json, muleContext);

        List<Person> people = message.getPayload(new ListDataType<List<Person>>(Person.class));
        assertNotNull(people);
        assertEquals(2, people.size());
    }
    
    @Test
    public void testCustomTransformerTakesPrecedence() throws Exception
    {
        DataType resultDataType = DataTypeFactory.create(String.class, "application/json");
        DataType sourceDataType = DataTypeFactory.create(League.class);

        muleContext.getRegistry().registerObject("customTransformer",new TransformerProvider());
        Transformer transformer = muleContext.getRegistry().lookupTransformer(sourceDataType, resultDataType);
        
        assertThat(transformer, org.hamcrest.core.IsInstanceOf.instanceOf(AnnotatedTransformerProxy.class));
        MuleMessage message = new DefaultMuleMessage(new League(), muleContext);
        assertThat(message.getPayload(String.class), is(TransformerProvider.RESULT));
    }

    @JsonAutoDetect
    public static class League {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @ContainsTransformerMethods
    public static class TransformerProvider
    {

        public static final String RESULT = "I am a league";

        @org.mule.api.annotations.Transformer
        public String convert(League payload)
        {
            return RESULT;
        }
    }
}
