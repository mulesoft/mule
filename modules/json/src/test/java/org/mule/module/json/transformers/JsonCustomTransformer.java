/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.transformers;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.json.model.EmailAddress;
import org.mule.json.model.Item;
import org.mule.json.model.Person;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ContainsTransformerMethods
public class JsonCustomTransformer
{
    //This is used to test other source types and injecting an ObjectMapper instance
    @Transformer(sourceTypes = String.class)
    public Person toCar(byte[] doc, ObjectMapper context) throws IOException
    {
        return context.readValue(doc, 0, doc.length, Person.class);
    }


    //NOTE the @MessagePayload annotation is ignored for transformer but we're just testing that that it doesn't break things
    @Transformer
    public EmailAddress toEmail(@Payload InputStream in, @InboundHeaders("*") Map headers, ObjectMapper mapper) throws IOException
    {
        Object foo = headers.get("foo");
        if(foo==null || !"fooValue".equals(foo))
        {
            throw new IllegalArgumentException("Header foo not set to 'fooValue'");
        }
        return mapper.readValue(in, EmailAddress.class);
    }


    @Transformer(sourceTypes = {InputStream.class})
    public List<Item> toItemList(@Payload String in, ObjectMapper mapper) throws IOException
    {
        List<Item> items = new ArrayList<Item>();
        ArrayNode nodes = (ArrayNode) mapper.readTree(in);
        for (Iterator<JsonNode> iterator = nodes.getElements(); iterator.hasNext();)
        {
            //TODO, we're reparsing content here
            items.add(mapper.readValue(iterator.next().toString(), Item.class));
        }

        return items;
    }

    @Transformer(sourceTypes = {InputStream.class})
    public List<Person> toPeople(@Payload String in, ObjectMapper mapper) throws IOException
    {
        List<Person> people = new ArrayList<Person>();
        ArrayNode nodes = (ArrayNode) mapper.readTree(in);
        for (Iterator<JsonNode> iterator = nodes.getElements(); iterator.hasNext();)
        {
            //TODO, we're reparsing content here
             people.add(mapper.readValue(iterator.next().toString(), Person.class));
        }

        return people;
    }
}
