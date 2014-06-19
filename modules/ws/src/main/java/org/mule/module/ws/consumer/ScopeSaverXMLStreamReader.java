/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import org.mule.module.xml.stax.DelegateXMLStreamReader;
import org.mule.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;

import javanet.staxutils.events.EventAllocator;

/**
 * XMLStreamReader decorator that saves the scope and allows accesing to information about all the parent XMLEvent elements
 */
public class ScopeSaverXMLStreamReader extends DelegateXMLStreamReader
{

    private final XMLEventAllocator allocator = new EventAllocator();
    private List<StartElement> scope = new ArrayList<StartElement>();
    private boolean first = true;

    public ScopeSaverXMLStreamReader(XMLStreamReader reader)
    {
        super(reader);
    }

    @Override
    public int next() throws XMLStreamException
    {
        if (first)
        {
            first = false;
            if (getEventType() == XMLStreamReader.START_ELEMENT)
            {
                XMLEvent evt = allocator.allocate(this);
                scope.add(evt.asStartElement());
            }
        }
        int res = super.next();
        switch(res)
        {
            case XMLStreamReader.START_ELEMENT:
                XMLEvent evt = allocator.allocate(this);
                scope.add(evt.asStartElement());
                break;
            case XMLStreamReader.END_ELEMENT:
                scope.remove(scope.size() - 1);
                break;
            default:
                break;
        }
        return res;
    }

    public StartElement currentScope()
    {
        Preconditions.checkState(scope.size() > 0, "No scope available");
        return scope.get(scope.size() - 1);
    }

    public List<StartElement> scopes() {
        return scope;
    }

}
