/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.jaxb;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.jaxb.model.EmailAddress;
import org.mule.jaxb.model.Person;
import org.mule.module.xml.util.XMLUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;



/**
 * Explicit JAXB transformers used to test that JAXB transforms can be intercepted
 */
@ContainsTransformerMethods
public class JAXBTestTransformers
{

    @Transformer(sourceTypes = {String.class})
    public Person toPerson(Document doc, JAXBContext context) throws JAXBException
    {
        return (Person) context.createUnmarshaller().unmarshal(doc);
    }


    //NOTE the @MessagePayload annotation is ignored for transformer but we're just testing that that it doesn't break things
    @Transformer(sourceTypes = {String.class, InputStream.class})
    public List<EmailAddress> toEmailAddresses(@Payload Document doc, @InboundHeaders("*") Map headers, JAXBContext context) throws JAXBException, XPathExpressionException
    {
        //Test that we receive headers

        if(!headers.get("foo").equals("fooValue"))
        {
            throw new IllegalArgumentException("Header foo was not set to the correct value 'fooValue'");
        }
        
        List<Node> nodes = XMLUtils.select("/person/emailAddresses/emailAddress", doc);
        List<EmailAddress> addrs = new ArrayList<EmailAddress>(nodes.size());
        for (Node node : nodes)
        {
            addrs.add(context.createUnmarshaller().unmarshal(node, EmailAddress.class).getValue());
        }
        return addrs;
    }

}
