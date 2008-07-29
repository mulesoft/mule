/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.stax;

import java.util.ArrayList;
import java.util.List;

import javanet.staxutils.events.AttributeEvent;
import javanet.staxutils.events.CDataEvent;
import javanet.staxutils.events.CharactersEvent;
import javanet.staxutils.events.CommentEvent;
import javanet.staxutils.events.EndDocumentEvent;
import javanet.staxutils.events.EndElementEvent;
import javanet.staxutils.events.NamespaceEvent;
import javanet.staxutils.events.StartDocumentEvent;
import javanet.staxutils.events.StartElementEvent;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

public class ReversibleXMLStreamReader extends DelegateXMLStreamReader
{
    private final static int MODE_NORMAL = 0;
    private final static int MODE_TRACKING = 1;
    private final static int MODE_REPLAY = 2;

    private int mode = MODE_NORMAL;
    private List events;
    private XMLEvent current;
    private int replayIndex;

    public ReversibleXMLStreamReader(XMLStreamReader reader)
    {
        super(reader);
    }

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
        mode = MODE_REPLAY;
    }

    public boolean isTracking()
    {
        return mode == MODE_TRACKING;
    }

    public void setTracking(boolean tracking)
    {
        if (tracking)
        {
            mode = MODE_TRACKING;
            replayIndex = 0;
            events = new ArrayList();
        }
        else
        {
            mode = MODE_NORMAL;
        }
    }

    public int next() throws XMLStreamException
    {
        int event;

        if (mode == MODE_TRACKING)
        {
            event = super.next();
            capture(event);
        }
        else if (mode == MODE_REPLAY)
        {
            if (replayIndex == events.size())
            {
                mode = MODE_NORMAL;
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

        return event;
    }

    private int getReplayEvent()
    {
        current = (XMLEvent) events.get(replayIndex);
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
                events.add(new EndElementEvent(getName(), getNamespaces().iterator(), getLocation()));
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
        List attributes = new ArrayList();
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

    private List getNamespaces()
    {
        List namespaces = new ArrayList();
        for (int i = 0; i < getNamespaceCount(); i++)
        {
            namespaces.add(new NamespaceEvent(getNamespacePrefix(i), getNamespaceURI(i), getLocation()));
        }
        return namespaces;
    }

    public String getElementText() throws XMLStreamException
    {
        if (getEventType() != XMLStreamConstants.START_ELEMENT)
        {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", getLocation());
        }

        int eventType = next();
        StringBuffer buf = new StringBuffer();
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

    public int getAttributeCount()
    {
        if (mode == MODE_REPLAY)
        {
            return ((StartElementEventX) current).getAttributeList().size();
        }
        else
        {
            return super.getAttributeCount();
        }
    }

    public String getAttributeLocalName(int i)
    {

        if (mode == MODE_REPLAY)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getName().getLocalPart();
        }
        else 
        {
            return super.getAttributeLocalName(i);
        }
    }

    public QName getAttributeName(int i)
    {

        if (mode == MODE_REPLAY)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getName();
        }
        else
        {
            return super.getAttributeName(i);
        }
    }

    public String getAttributeNamespace(int i)
    {

        if (mode == MODE_REPLAY)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getName().getNamespaceURI();
        }
        else
        {
            return super.getAttributeNamespace(i);
        }
    }

    public String getAttributePrefix(int i)
    {
        if (mode == MODE_REPLAY)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getName().getPrefix();
        }
        else
        {
            return super.getAttributePrefix(i);
        }
    }

    public String getAttributeType(int i)
    {
        if (mode == MODE_REPLAY)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getDTDType();
        }
        else
        {
            return super.getAttributeType(i);
        }
    }

    public String getAttributeValue(int i)
    {
        if (mode == MODE_REPLAY)
        {
            Attribute att = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return att.getValue();
        }
        else
        {
            return super.getAttributeValue(i);
        }
    }

    public String getAttributeValue(String ns, String local)
    {
        if (mode == MODE_REPLAY)
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

    public int getEventType()
    {
        if (mode == MODE_REPLAY)
        {
            return current.getEventType();
        }
        else
        {
            return super.getEventType();
        }
    }

    public String getLocalName()
    {
        if (mode == MODE_REPLAY)
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

    public Location getLocation()
    {
        if (mode == MODE_REPLAY)
        {
            return current.getLocation();
        }
        else
        {
            return super.getLocation();
        }
    }

    public QName getName()
    {
        if (mode == MODE_REPLAY)
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

    public NamespaceContext getNamespaceContext()
    {
        if (mode == MODE_REPLAY)
        {
            return ((StartElementEventX) current).getNamespaceContext();
        }
        else
        {
            return super.getNamespaceContext();
        }
    }

    public int getNamespaceCount()
    {
        if (mode == MODE_REPLAY)
        {
            return ((StartElementEventX) current).getNamespaceList().size();
        }
        else
        {
            return super.getNamespaceCount();
        }
    }

    public String getNamespacePrefix(int arg0)
    {
        if (mode == MODE_REPLAY)
        {
            Namespace ns = (Namespace) ((StartElementEventX) current).getNamespaceList().get(arg0);
            
            return ns.getPrefix();
        }
        else
        {
            return super.getNamespacePrefix(arg0);
        }
    }

    public String getNamespaceURI()
    {
        if (mode == MODE_REPLAY)
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

    public String getNamespaceURI(int arg0)
    {
        if (mode == MODE_REPLAY)
        {
            Namespace ns = (Namespace) ((StartElementEventX) current).getNamespaceList().get(arg0);
            
            return ns.getNamespaceURI();
        }
        else
        {
            return super.getNamespaceURI(arg0);
        }
    }

    public String getNamespaceURI(String prefix)
    {
        if (mode == MODE_REPLAY)
        {
            return ((StartElementEventX) current).getNamespaceURI(prefix);
        }
        else
        {
            return super.getNamespaceURI(prefix);
        }
    }

    public String getPIData()
    {
        if (mode == MODE_REPLAY)
        {
            return null;
        }
        else
        {
            return super.getPIData();
        }
    }

    public String getPITarget()
    {
        if (mode == MODE_REPLAY)
        {
            return null;
        }
        else
        {
            return super.getPITarget();
        }
    }

    public String getPrefix()
    {
        if (mode == MODE_REPLAY)
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
            return super.getPrefix();
        }
    }

    public String getText()
    {
        if (mode == MODE_REPLAY)
        {
            return ((CharactersEvent) current).getData();
        }
        else
        {
            return super.getText();
        }
    }

    public char[] getTextCharacters()
    {
        if (mode == MODE_REPLAY)
        {
            return ((CharactersEvent) current).getData().toCharArray();
        }
        else
        {
            return super.getTextCharacters();
        }
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException
    {
        if (mode == MODE_REPLAY)
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

    public int getTextLength()
    {
        if (mode == MODE_REPLAY)
        {
            return getText().length();
        }
        else
        {
            return super.getTextLength();
        }
    }

    public int getTextStart()
    {
        if (mode == MODE_REPLAY)
        {
            return 0;
        }
        else
        {
            return super.getTextStart();
        }
    }

    public boolean hasName()
    {
        if (mode == MODE_REPLAY)
        {
            return isStartElement() || isEndElement();
        }
        else
        {
            return super.hasName();
        }
    }

    public boolean hasNext() throws XMLStreamException
    {
        if (mode == MODE_REPLAY)
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

    public boolean hasText()
    {
        if (mode == MODE_REPLAY)
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

    public boolean isAttributeSpecified(int i)
    {
        if (mode == MODE_REPLAY)
        {
            Attribute attr = (Attribute) ((StartElementEventX) current).getAttributeList().get(i);
            return attr.isSpecified();
        }
        else
        {
            return super.isAttributeSpecified(i);
        }
    }

    public boolean isCharacters()
    {
        if (mode == MODE_REPLAY)
        {
            return current.isCharacters();
        }
        else
        {
            return super.isCharacters();
        }
    }

    public boolean isEndElement()
    {
        if (mode == MODE_REPLAY)
        {
            return current.isEndElement();
        }
        else
        {
            return super.isEndElement();
        }
    }


    public boolean isStartElement()
    {
        if (mode == MODE_REPLAY)
        {
            return current.isStartElement();
        }
        else
        {
            return super.isStartElement();
        }
    }

    public boolean isWhiteSpace()
    {
        if (mode == MODE_REPLAY)
        {
            return current.getEventType() == SPACE;
        }
        else
        {
            return super.isWhiteSpace();
        }
    }

}
