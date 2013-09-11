/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.mule.tck.junit4.FunctionalTestCase;

import java.io.IOException;
import java.util.Date;

import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Base;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.writer.Writer;

import static org.junit.Assert.assertEquals;

public abstract class AbstractCustomerTest extends FunctionalTestCase
{

    protected void testCustomerProvider(String basePath) throws Exception
    {
        Abdera abdera = new Abdera();
        Factory factory = abdera.getFactory();

        AbderaClient client = new AbderaClient(abdera);
        String base = "http://localhost:9002" + basePath + "/";

        // Testing of entry creation
        IRI colUri = new IRI(base).resolve("customers");
        Entry entry = factory.newEntry();
        entry.setTitle("Hmmm this is ignored right now");
        entry.setUpdated(new Date());
        entry.addAuthor("Acme Industries");
        entry.setId(factory.newUuidUri());
        entry.setSummary("Customer document");

        Element customerEl = factory.newElement(new QName("customer"));
        customerEl.setAttributeValue(new QName("name"), "Dan Diephouse");
        entry.setContent(customerEl);

        RequestOptions opts = new RequestOptions();
        opts.setContentType("application/atom+xml;type=entry");
        ClientResponse res = client.post(colUri.toString(), entry, opts);
        assertEquals(201, res.getStatus());

        IRI location = res.getLocation();
        assertEquals(basePath + "/customers/1001-Dan_Diephouse", location.toString());

        // GET the entry
        res = client.get(colUri.resolve(location.toString()).toString());
        assertEquals(200, res.getStatus());

        org.apache.abdera.model.Document<Entry> entry_doc = res.getDocument();
        entry = entry_doc.getRoot();
    }

    protected void prettyPrint(Abdera abdera, Base doc) throws IOException
    {
        Writer writer = abdera.getWriterFactory().getWriter("prettyxml");
        writer.writeTo(doc, System.out);
        System.out.println();
    }
}
