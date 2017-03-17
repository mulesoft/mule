/* $Id$
 *
 * Copyright (c) 2004, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.mule.extension.ws.internal.xml.stax;

import javanet.staxutils.DummyLocator;
import javanet.staxutils.StAXReaderToContentHandler;
import javanet.staxutils.helpers.XMLFilterImplEx;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This is a simple utility class that adapts StAX events from an {@link javax.xml.stream.XMLStreamReader} to SAX events on a
 * {@link org.xml.sax.ContentHandler}, bridging between the two parser technologies.
 * 
 * @author Ryan.Shoemaker@Sun.COM
 * @version 1.0
 */
public class XMLStreamReaderToContentHandler implements StAXReaderToContentHandler {

  // StAX event source
  private final XMLStreamReader staxStreamReader;

  // SAX event sinks
  private XMLFilterImplEx filter;

  /**
   * Construct a new StAX to SAX adapter that will convert a StAX event stream into a SAX event stream.
   * 
   * @param staxCore StAX event source
   * @param filter SAX event sink
   */
  public XMLStreamReaderToContentHandler(XMLStreamReader staxCore, XMLFilterImplEx filter) {
    staxStreamReader = staxCore;

    this.filter = filter;
  }

  public void bridge() throws XMLStreamException {
    try {
      // remembers the nest level of elements to know when we are done.
      int depth = 0;
      boolean isDocument = false;

      handleStartDocument();

      // if the parser is at the start document, procees any comments or PIs
      int event = staxStreamReader.getEventType();
      if (event == XMLStreamConstants.START_DOCUMENT) {
        isDocument = true;
        event = staxStreamReader.next();
        while (event != XMLStreamConstants.START_ELEMENT) {
          switch (event) {
            case XMLStreamConstants.COMMENT:
              handleComment();
              break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
              handlePI();
              break;
          }
          event = staxStreamReader.next();
        }
      }

      if (event != XMLStreamConstants.START_ELEMENT)
        throw new IllegalStateException("The current event is not START_ELEMENT\n but" + event);

      do {
        // These are all of the events listed in the javadoc for
        // XMLEvent.
        // The spec only really describes 11 of them.
        switch (event) {
          case XMLStreamConstants.START_ELEMENT:
            depth++;
            handleStartElement();
            break;
          case XMLStreamConstants.END_ELEMENT:
            handleEndElement();
            depth--;
            break;
          case XMLStreamConstants.CHARACTERS:
            handleCharacters();
            break;
          case XMLStreamConstants.ENTITY_REFERENCE:
            handleEntityReference();
            break;
          case XMLStreamConstants.PROCESSING_INSTRUCTION:
            handlePI();
            break;
          case XMLStreamConstants.COMMENT:
            handleComment();
            break;
          case XMLStreamConstants.DTD:
            handleDTD();
            break;
          case XMLStreamConstants.ATTRIBUTE:
            handleAttribute();
            break;
          case XMLStreamConstants.NAMESPACE:
            handleNamespace();
            break;
          case XMLStreamConstants.CDATA:
            handleCDATA();
            break;
          case XMLStreamConstants.ENTITY_DECLARATION:
            handleEntityDecl();
            break;
          case XMLStreamConstants.NOTATION_DECLARATION:
            handleNotationDecl();
            break;
          case XMLStreamConstants.SPACE:
            handleSpace();
            break;
          default:
            throw new InternalError("processing event: " + event);
        }

        event = staxStreamReader.next();
      } while (depth != 0);

      // procees any remaining comments or PIs
      if (isDocument) {
        while (event != XMLStreamConstants.END_DOCUMENT) {
          switch (event) {
            case XMLStreamConstants.COMMENT:
              handleComment();
              break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
              handlePI();
              break;
          }
          event = staxStreamReader.next();
        }
      }

      handleEndDocument();
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  protected void handleEndDocument() throws SAXException {
    filter.endDocument();
  }

  protected void handleStartDocument() throws SAXException {
    final Location location = staxStreamReader.getLocation();
    if (location != null) {
      filter.setDocumentLocator(new Locator() {

        public int getColumnNumber() {
          return location.getColumnNumber();
        }

        public int getLineNumber() {
          return location.getLineNumber();
        }

        public String getPublicId() {
          return location.getPublicId();
        }

        public String getSystemId() {
          return location.getSystemId();
        }
      });
    } else {
      filter.setDocumentLocator(new DummyLocator());
    }
    filter.startDocument();
  }

  protected void handlePI() throws XMLStreamException {
    try {
      filter.processingInstruction(staxStreamReader.getPITarget(), staxStreamReader.getPIData());
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  protected void handleCharacters() throws XMLStreamException {
    char[] chars = staxStreamReader.getText().toCharArray();

    try {
      filter.characters(chars, 0, chars.length);
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  protected void handleEndElement() throws XMLStreamException {
    QName qName = staxStreamReader.getName();

    try {
      // fire endElement
      String prefix = qName.getPrefix();
      String rawname;
      if (prefix == null || prefix.length() == 0)
        rawname = qName.getLocalPart();
      else
        rawname = prefix + ':' + qName.getLocalPart();

      filter.endElement(qName.getNamespaceURI(), qName.getLocalPart(), rawname);

      // end namespace bindings
      int nsCount = staxStreamReader.getNamespaceCount();
      for (int i = nsCount - 1; i >= 0; i--) {
        String nsprefix = staxStreamReader.getNamespacePrefix(i);
        if (nsprefix == null) { // true for default namespace
          nsprefix = "";
        }
        filter.endPrefixMapping(nsprefix);
      }
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  protected void handleStartElement() throws XMLStreamException {

    try {
      // start namespace bindings
      int nsCount = staxStreamReader.getNamespaceCount();
      for (int i = 0; i < nsCount; i++) {
        String uri = staxStreamReader.getNamespaceURI(i);
        if (uri == null) {
          uri = "";
        }
        String prefix = staxStreamReader.getNamespacePrefix(i);
        if (prefix == null) { // true for default namespace
          prefix = "";
        }
        filter.startPrefixMapping(prefix, uri);
      }

      // fire startElement
      QName qName = staxStreamReader.getName();
      String prefix = qName.getPrefix();
      String rawname;
      if (prefix == null || prefix.length() == 0)
        rawname = qName.getLocalPart();
      else
        rawname = prefix + ':' + qName.getLocalPart();
      Attributes attrs = getAttributes();
      filter.startElement(qName.getNamespaceURI(), qName.getLocalPart(), rawname, attrs);
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  /**
   * Get the attributes associated with the given START_ELEMENT or ATTRIBUTE StAXevent.
   * 
   * @return the StAX attributes converted to an org.xml.sax.Attributes
   */
  protected Attributes getAttributes() {
    AttributesImpl attrs = new AttributesImpl();

    int eventType = staxStreamReader.getEventType();
    if (eventType != XMLStreamConstants.ATTRIBUTE && eventType != XMLStreamConstants.START_ELEMENT) {
      throw new InternalError("getAttributes() attempting to process: " + eventType);
    }

    // Add namspace declarations if required
    if (filter.getNamespacePrefixes()) {
      for (int i = 0; i < staxStreamReader.getNamespaceCount(); i++) {
        String uri = staxStreamReader.getNamespaceURI(i);
        if (uri == null)
          uri = "";

        String prefix = staxStreamReader.getNamespacePrefix(i);
        if (prefix == null)
          prefix = "";

        String qName = "xmlns";
        if (prefix.length() == 0) {
          prefix = qName;
        } else {
          qName = qName + ':' + prefix;
        }
        attrs.addAttribute("http://www.w3.org/2000/xmlns/", prefix, qName, "CDATA", uri);
      }
    }

    // gather non-namespace attrs
    for (int i = 0; i < staxStreamReader.getAttributeCount(); i++) {
      String uri = staxStreamReader.getAttributeNamespace(i);
      if (uri == null)
        uri = "";
      String localName = staxStreamReader.getAttributeLocalName(i);
      String prefix = staxStreamReader.getAttributePrefix(i);
      String qName;
      if (prefix == null || prefix.length() == 0)
        qName = localName;
      else
        qName = prefix + ':' + localName;
      String type = staxStreamReader.getAttributeType(i);
      String value = staxStreamReader.getAttributeValue(i);

      attrs.addAttribute(uri, localName, qName, type, value);
    }

    return attrs;
  }

  protected void handleNamespace() {
    // no-op ???
    // namespace events don't normally occur outside of a startElement
    // or endElement
  }

  protected void handleAttribute() {
    // no-op ???
    // attribute events don't normally occur outside of a startElement
    // or endElement
  }

  protected void handleDTD() {
    // no-op ???
    // it seems like we need to pass this info along, but how?
  }

  protected void handleComment() throws XMLStreamException {
    char[] chars = staxStreamReader.getText().toCharArray();

    try {
      filter.comment(chars, 0, chars.length);
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  protected void handleEntityReference() {
    // no-op ???
  }

  protected void handleSpace() {
    // no-op ???
    // this event is listed in the javadoc, but not in the spec.
  }

  protected void handleNotationDecl() {
    // no-op ???
    // this event is listed in the javadoc, but not in the spec.
  }

  protected void handleEntityDecl() {
    // no-op ???
    // this event is listed in the javadoc, but not in the spec.
  }

  protected void handleCDATA() throws XMLStreamException {
    char[] chars = staxStreamReader.getText().toCharArray();

    try {
      filter.startCDATA();
      filter.characters(chars, 0, chars.length);
      filter.endCDATA();
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }
}
