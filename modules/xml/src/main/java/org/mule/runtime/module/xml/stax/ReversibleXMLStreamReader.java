/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.stax;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

import javanet.staxutils.events.AbstractCharactersEvent;
import javanet.staxutils.events.AttributeEvent;
import javanet.staxutils.events.CDataEvent;
import javanet.staxutils.events.CharactersEvent;
import javanet.staxutils.events.CommentEvent;
import javanet.staxutils.events.EndDocumentEvent;
import javanet.staxutils.events.EndElementEvent;
import javanet.staxutils.events.NamespaceEvent;
import javanet.staxutils.events.StartDocumentEvent;
import javanet.staxutils.events.StartElementEvent;

public class ReversibleXMLStreamReader extends DelegateXMLStreamReader
{
    private List<XMLEvent> events;
    private XMLEvent current;
    private int replayIndex;
    private boolean tracking = false;
    private boolean replay = false;

    public ReversibleXMLStreamReader(XMLStreamReader reader)
    {
        super(reader);
    }

    @Override
    public int nextTag() throws XMLStreamException
    {
        int eventType = next();
        while ((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace())
               || (eventType == XMLStreamConstants.CDATA && isWhiteSpace())
               || eventType == XMLStreamConstants.SPACE
               || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
               || eventType == XMLStreamConstants.COMMENT)
        {
            eventType = next();
        }
        if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT)
        {
            throw new XMLStreamException("expected start or end tag", getLocation());
        }
        return eventType;
    }

    public void reset()
    {
        replay = true;
        replayIndex = 0;
        current = null;
    }

    public boolean isTracking()
    {
        return tracking;
    }

    public void setTracking(boolean tracking)
    {
        this.tracking = tracking;

        if (tracking)
        {
            replayIndex = 0;

            if (events == null)
            {
                events = new ArrayList<XMLEvent>();
            }
        }
    }

    @Override
    public int next() throws XMLStreamException
    {
        int event;

        if (replay)
        {
            if (replayIndex == events.size())
            {
                replay = false;
                event = super.next();
            }
            else
            {
                event = getReplayEvent();
            }
        }
        else
        {
            event = super.next();
        }

        if (tracking && !replay)
        {
            capture(event);
        }

        return event;
    }

    private int getReplayEvent()
    {
        current = events.get(replayIndex);
        replayIndex++;

        return current.getEventType();
    }

    /**
     * Capture the current event;
     *
     * @param event
     */
    private void capture(int event)
    {
        switch (event)
        {
            case XMLStreamConstants.START_DOCUMENT :
                events.add(new StartDocumentEvent(getEncoding(), new Boolean(isStandalone()), getVersion(),
                    getLocation()));
                break;
            case XMLStreamConstants.END_DOCUMENT :
                events.add(new EndDocumentEvent(getLocation()));
                break;
            case XMLStreamConstants.START_ELEMENT :
                events.add(createStartElementEvent());
                break;
            case XMLStreamConstants.END_ELEMENT :
                events.add(new EndElementEventX(getName(), getNamespaces()));
                break;
            case XMLStreamConstants.CDATA :
                events.add(new CDataEvent(getText(), getLocation()));
                break;
            case XMLStreamConstants.CHARACTERS :
                events.add(new CharactersEvent(getText(), getLocation()));
                break;
            case XMLStreamConstants.COMMENT :
                events.add(new CommentEvent(getText(), getLocation()));
                break;
            case XMLStreamConstants.DTD :
                break;
            case XMLStreamConstants.ENTITY_DECLARATION :
                break;
            case XMLStreamConstants.ENTITY_REFERENCE :
                break;
            case XMLStreamConstants.NOTATION_DECLARATION :
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION :
                break;
            case XMLStreamConstants.SPACE :
                break;
        }
    }

    private StartElementEvent createStartElementEvent()
    {
        List<AttributeEvent> attributes = new ArrayList<AttributeEvent>();
        for (int i = 0; i < getAttributeCount(); i++)
        {
            attributes.add(new AttributeEvent(getAttributeName(i), getAttributeValue(i)));
        }

        return new StartElementEventX(getName(), attributes, getNamespaces(),
            createContext(), getLocation(), null);
    }

    private NamespaceContext createContext()
    {
        MapNamespaceContext ctx = new MapNamespaceContext();

        for (int i = 0; i < getNamespaceCount(); i++)
        {
            ctx.addNamespace(getNamespacePrefix(i), getNamespaceURI(i));
        }

        return ctx;
    }

    private List<Namespace> getNamespaces()
    {
        List<Namespace> namespaces = new ArrayList<Namespace>();
        for (int i = 0; i < getNamespaceCount(); i++)
        {
            namespaces.add(new NamespaceEvent(getNamespacePrefix(i), getNamespaceURI(i), getLocation()));
        }
        return namespaces;
    }

    @Override
    public String getElementText() throws XMLStreamException
    {
        if (getEventType() != XMLStreamConstants.START_ELEMENT)
        {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", getLocation());
        }

        int eventType = next();
        StringBuilder buf = new StringBuilder();
        while (eventType != XMLStreamConstants.END_ELEMENT)
        {
            if (eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA
                || eventType == XMLStreamConstants.SPACE || eventType == XMLStreamConstants.ENTITY_REFERENCE)
            {
                buf.append(getText());
            }
            else if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                     || eventType == XMLStreamConstants.COMMENT)
            {
                // skipping
            }
            else if (eventType == XMLStreamConstants.END_DOCUMENT)
            {
                throw new XMLStreamException("unexpected end of document when reading element text content");
            }
            else if (eventType == XMLStreamConstants.START_ELEMENT)
            {
                throw new XMLStreamException("element text content may not contain START_ELEMENT",
                    getLocation());
            }
            else
            {
                throw new XMLStreamException("Unexpected event type " + eventType, getLocation());
            }
            eventType = next();
        }
        return buf.toString();

    }

    @Override
    public int getAttributeCount()
    {
        if (replay)
        {
            return ((StartElementEventX) current).getAttributeList().size();
        }
        else
        {
            return super.getAttributeCount();
        }
    }

    @Override
    public String getAttributeLocalName(int i)
    {

        if (replay)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getName().getLocalPart();
        }
        else
        {
            return super.getAttributeLocalName(i);
        }
    }

    @Override
    public QName getAttributeName(int i)
    {

        if (replay)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getName();
        }
        else
        {
            return super.getAttributeName(i);
        }
    }

    @Override
    public String getAttributeNamespace(int i)
    {

        if (replay)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getName().getNamespaceURI();
        }
        else
        {
            return super.getAttributeNamespace(i);
        }
    }

    @Override
    public String getAttributePrefix(int i)
    {
        if (replay)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getName().getPrefix();
        }
        else
        {
            return super.getAttributePrefix(i);
        }
    }

    @Override
    public String getAttributeType(int i)
    {
        if (replay)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getDTDType();
        }
        else
        {
            return super.getAttributeType(i);
        }
    }

    @Override
    public String getAttributeValue(int i)
    {
        if (replay)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getValue();
        }
        else
        {
            return super.getAttributeValue(i);
        }
    }

    @Override
    public String getAttributeValue(String ns, String local)
    {
        if (replay)
        {
            Attribute att = ((StartElementEventX) current).getAttributeByName(new QName(ns, local));
            if (att != null)
            {
                return att.getValue();
            }
            else
            {
                return null;
            }
        }
        else
        {
            return super.getAttributeValue(ns, local);
        }
    }

    @Override
    public int getEventType()
    {
        if (replay)
        {
            if (current == null)
            {
                return -1;
            }
            return current.getEventType();
        }
        else
        {
            return super.getEventType();
        }
    }

    @Override
    public String getLocalName()
    {
        if (replay)
        {
            if (isStartElement())
            {
                return ((StartElementEventX) current).getName().getLocalPart();
            }
            else
            {
                return ((EndElementEvent) current).getName().getLocalPart();
            }
        }
        else
        {
            return super.getLocalName();
        }
    }

    @Override
    public Location getLocation()
    {
        if (replay)
        {
            return current.getLocation();
        }
        else
        {
            return super.getLocation();
        }
    }

    @Override
    public QName getName()
    {
        if (replay)
        {
            if (isStartElement())
            {
                return ((StartElementEventX) current).getName();
            }
            else
            {
                return ((EndElementEvent) current).getName();
            }
        }
        else
        {
            return super.getName();
        }
    }

    @Override
    public NamespaceContext getNamespaceContext()
    {
        if (replay)
        {
            return ((StartElementEventX) current).getNamespaceContext();
        }
        else
        {
            return super.getNamespaceContext();
        }
    }

    @Override
    public int getNamespaceCount()
    {
        if (replay)
        {
            if(isStartElement())
            {
                return ((StartElementEventX) current).getNamespaceList().size();
            }
            else
            {
                return ((EndElementEventX) current).getNamespaceList().size();
            }
        }
        else
        {
            return super.getNamespaceCount();
        }
    }

    @Override
    public String getNamespacePrefix(int arg0)
    {
        if (replay)
        {
            if(isStartElement())
            {
                Namespace ns = (Namespace) ((StartElementEventX) current).getNamespaceList().get(arg0);

                return ns.getPrefix();
            }
            else
            {
                Namespace ns = ((EndElementEventX) current).getNamespaceList().get(arg0);

                return ns.getPrefix();
            }
        }
        else
        {
            return super.getNamespacePrefix(arg0);
        }
    }

    @Override
    public String getNamespaceURI()
    {
        if (replay)
        {
            if (isStartElement())
            {
                return ((StartElementEventX) current).getName().getNamespaceURI();
            }
            else
            {
                return ((EndElementEvent) current).getName().getNamespaceURI();
            }
        }
        else
        {
            return super.getNamespaceURI();
        }
    }

    @Override
    public String getNamespaceURI(int arg0)
    {
        if (replay)
        {
            Namespace ns = (Namespace) ((StartElementEventX) current).getNamespaceList().get(arg0);

            return ns.getNamespaceURI();
        }
        else
        {
            return super.getNamespaceURI(arg0);
        }
    }

    @Override
    public String getNamespaceURI(String prefix)
    {
        if (replay)
        {
            return ((StartElementEventX) current).getNamespaceURI(prefix);
        }
        else
        {
            return super.getNamespaceURI(prefix);
        }
    }

    @Override
    public String getPIData()
    {
        if (replay)
        {
            return null;
        }
        else
        {
            return super.getPIData();
        }
    }

    @Override
    public String getPITarget()
    {
        if (replay)
        {
            return null;
        }
        else
        {
            return super.getPITarget();
        }
    }

    @Override
    public String getPrefix()
    {
        if (replay)
        {
            if (isStartElement())
            {
                return ((StartElementEventX) current).getName().getPrefix();
            }
            else
            {
                return ((EndElementEvent) current).getName().getPrefix();
            }
        }
        else
        {
            return super.getPrefix();
        }
    }

    @Override
    public String getText()
    {
        if (replay)
        {
            if (current instanceof CommentEvent)
            {
                return ((CommentEvent) current).getText();
            }
            else
            {
                return ((AbstractCharactersEvent) current).getData();
            }
        }
        else
        {
            return super.getText();
        }
    }

    @Override
    public char[] getTextCharacters()
    {
        if (replay)
        {
            return ((CharactersEvent) current).getData().toCharArray();
        }
        else
        {
            return super.getTextCharacters();
        }
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException
    {
        if (replay)
        {
            char[] src = getText().toCharArray();

            if (sourceStart + length >= src.length) {
                length = src.length - sourceStart;
            }

            for (int i = 0; i < length; i++) {
                target[targetStart + i] = src[i + sourceStart];
            }

            return length;
        }
        else
        {
            return super.getTextCharacters(sourceStart, target, targetStart, length);
        }
    }

    @Override
    public int getTextLength()
    {
        if (replay)
        {
            return getText().length();
        }
        else
        {
            return super.getTextLength();
        }
    }

    @Override
    public int getTextStart()
    {
        if (replay)
        {
            return 0;
        }
        else
        {
            return super.getTextStart();
        }
    }

    @Override
    public boolean hasName()
    {
        if (replay)
        {
            return isStartElement() || isEndElement();
        }
        else
        {
            return super.hasName();
        }
    }

    @Override
    public boolean hasNext() throws XMLStreamException
    {
        if (replay)
        {
            if (replayIndex == events.size())
            {
                return super.hasNext();
            }
            else
            {
                return true;
            }
        }
        else
        {
            return super.hasNext();
        }
    }

    @Override
    public boolean hasText()
    {
        if (replay)
        {
            int event = getEventType();
            return event == CHARACTERS || event == DTD
                || event == ENTITY_REFERENCE || event == COMMENT || event == SPACE;
        }
        else
        {
            return super.hasText();
        }
    }

    @Override
    public boolean isAttributeSpecified(int i)
    {
        if (replay)
        {
            Attribute attr = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return attr.isSpecified();
        }
        else
        {
            return super.isAttributeSpecified(i);
        }
    }

    @Override
    public boolean isCharacters()
    {
        if (replay)
        {
            return current.isCharacters();
        }
        else
        {
            return super.isCharacters();
        }
    }

    @Override
    public boolean isEndElement()
    {
        if (replay)
        {
            return current.isEndElement();
        }
        else
        {
            return super.isEndElement();
        }
    }


    @Override
    public boolean isStartElement()
    {
        if (replay)
        {
            return current != null && current.isStartElement();
        }
        else
        {
            return super.isStartElement();
        }
    }

    @Override
    public boolean isWhiteSpace()
    {
        if (replay)
        {
            if (current instanceof Characters)
            {
                return ((Characters) current).isWhiteSpace();
            }
            return current.getEventType() == SPACE;
        }
        else
        {
            return super.isWhiteSpace();
        }
    }

}
