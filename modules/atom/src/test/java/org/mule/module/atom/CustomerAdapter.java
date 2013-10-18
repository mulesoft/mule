/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Person;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.abdera.protocol.server.impl.AbstractEntityCollectionAdapter;

public class CustomerAdapter extends AbstractEntityCollectionAdapter<Customer>
{
    private static final String ID_PREFIX = "urn:acme:customer:";

    private AtomicInteger nextId = new AtomicInteger(1000);
    private Map<Integer, Customer> customers = new HashMap<Integer, Customer>();
    private Factory factory = new Abdera().getFactory();

    public String getId(RequestContext request)
    {
        return "tag:example.org,2007:feed";
    }

    public ResponseContext getCategories(RequestContext request)
    {
        return null;
    }

    @Override
    public Customer postEntry(String title, IRI id, String summary,
                              Date updated, List<Person> authors,
                              Content content, RequestContext request) throws ResponseContextException
    {
        Customer customer = contentToCustomer(content);
        customers.put(customer.getId(), customer);

        return customer;
    }

    private Customer contentToCustomer(Content content)
    {
        Customer customer = new Customer();

        return contentToCustomer(content, customer);
    }

    private Customer contentToCustomer(Content content, Customer customer)
    {
        Element firstChild = content.getFirstChild();
        customer.setName(firstChild.getAttributeValue("name"));
        customer.setId(nextId.incrementAndGet());
        return customer;
    }

    public void deleteEntry(String resourceName, RequestContext request) throws ResponseContextException
    {
        Integer id = getIdFromResourceName(resourceName);
        customers.remove(id);
    }

    public String getAuthor(RequestContext request)
    {
        return "Acme Industries";
    }

    @Override
    public List<Person> getAuthors(Customer entry, RequestContext request) throws ResponseContextException
    {
        Person author = request.getAbdera().getFactory().newAuthor();
        author.setName("Acme Industries");
        return Arrays.asList(author);
    }

    public Object getContent(Customer entry, RequestContext request)
    {
        Content content = factory.newContent();
        Element customerEl = factory.newElement(new QName("customer"));
        customerEl.setAttributeValue(new QName("name"), entry.getName());

        content.setValueElement(customerEl);
        return content;
    }

    public Iterable<Customer> getEntries(RequestContext request)
    {
        return customers.values();
    }

    public Customer getEntry(String resourceName, RequestContext request) throws ResponseContextException
    {
        Integer id = getIdFromResourceName(resourceName);
        return customers.get(id);
    }

    private Integer getIdFromResourceName(String resourceName) throws ResponseContextException
    {
        int idx = resourceName.indexOf("-");
        if (idx == -1)
        {
            throw new ResponseContextException(404);
        }
        Integer id = new Integer(resourceName.substring(0, idx));
        return id;
    }

    public Customer getEntryFromId(String id, RequestContext request)
    {
        return customers.get(new Integer(id));
    }

    public String getId(Customer entry)
    {
        // TODO: is this valid?
        return ID_PREFIX + entry.getId();
    }

    public String getName(Customer entry)
    {
        return entry.getId() + "-" + entry.getName().replaceAll(" ", "_");
    }

    public String getTitle(RequestContext request)
    {
        return "Acme Customer Database";
    }

    public String getTitle(Customer entry)
    {
        return entry.getName();
    }

    public Date getUpdated(Customer entry)
    {
        return new Date();
    }

    @Override
    public void putEntry(Customer entry, String title, Date updated,
                         List<Person> authors, String summary,
                         Content content, RequestContext request) throws ResponseContextException
    {
        contentToCustomer(content, entry);
    }

}
