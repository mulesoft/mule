/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.xml.stax;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @since 4.0, Copied from the removed XML module.
 */
public class DelegateXMLStreamReader implements XMLStreamReader {

  private XMLStreamReader reader;

  public DelegateXMLStreamReader(XMLStreamReader reader) {
    super();
    this.reader = reader;
  }

  @Override
  public void close() throws XMLStreamException {
    reader.close();
  }

  @Override
  public int getAttributeCount() {
    return reader.getAttributeCount();
  }

  @Override
  public String getAttributeLocalName(int arg0) {
    return reader.getAttributeLocalName(arg0);
  }

  @Override
  public QName getAttributeName(int arg0) {
    return reader.getAttributeName(arg0);
  }

  @Override
  public String getAttributeNamespace(int arg0) {
    return reader.getAttributeNamespace(arg0);
  }

  @Override
  public String getAttributePrefix(int arg0) {
    return reader.getAttributePrefix(arg0);
  }

  @Override
  public String getAttributeType(int arg0) {
    return reader.getAttributeType(arg0);
  }

  @Override
  public String getAttributeValue(int arg0) {
    return reader.getAttributeValue(arg0);
  }

  @Override
  public String getAttributeValue(String arg0, String arg1) {
    return reader.getAttributeValue(arg0, arg1);
  }

  @Override
  public String getCharacterEncodingScheme() {
    return reader.getCharacterEncodingScheme();
  }

  @Override
  public String getElementText() throws XMLStreamException {
    return reader.getElementText();
  }

  @Override
  public String getEncoding() {
    return reader.getEncoding();
  }

  @Override
  public int getEventType() {
    return reader.getEventType();
  }

  @Override
  public String getLocalName() {
    return reader.getLocalName();
  }

  @Override
  public Location getLocation() {
    return reader.getLocation();
  }

  @Override
  public QName getName() {
    return reader.getName();
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return reader.getNamespaceContext();
  }

  @Override
  public int getNamespaceCount() {
    return reader.getNamespaceCount();
  }

  @Override
  public String getNamespacePrefix(int arg0) {
    return reader.getNamespacePrefix(arg0);
  }

  @Override
  public String getNamespaceURI() {
    return reader.getNamespaceURI();
  }

  @Override
  public String getNamespaceURI(int arg0) {
    return reader.getNamespaceURI(arg0);
  }

  @Override
  public String getNamespaceURI(String arg0) {
    return reader.getNamespaceURI(arg0);
  }

  @Override
  public String getPIData() {
    return reader.getPIData();
  }

  @Override
  public String getPITarget() {
    return reader.getPITarget();
  }

  @Override
  public String getPrefix() {
    return reader.getPrefix();
  }

  @Override
  public Object getProperty(String arg0) throws IllegalArgumentException {
    return reader.getProperty(arg0);
  }

  @Override
  public String getText() {
    return reader.getText();
  }

  @Override
  public char[] getTextCharacters() {
    return reader.getTextCharacters();
  }

  @Override
  public int getTextCharacters(int arg0, char[] arg1, int arg2, int arg3) throws XMLStreamException {
    return reader.getTextCharacters(arg0, arg1, arg2, arg3);
  }

  @Override
  public int getTextLength() {
    return reader.getTextLength();
  }

  @Override
  public int getTextStart() {
    return reader.getTextStart();
  }

  @Override
  public String getVersion() {
    return reader.getVersion();
  }

  @Override
  public boolean hasName() {
    return reader.hasName();
  }

  @Override
  public boolean hasNext() throws XMLStreamException {
    return reader.hasNext();
  }

  @Override
  public boolean hasText() {
    return reader.hasText();
  }

  @Override
  public boolean isAttributeSpecified(int arg0) {
    return reader.isAttributeSpecified(arg0);
  }

  @Override
  public boolean isCharacters() {
    return reader.isCharacters();
  }

  @Override
  public boolean isEndElement() {
    return reader.isEndElement();
  }

  @Override
  public boolean isStandalone() {
    return reader.isStandalone();
  }

  @Override
  public boolean isStartElement() {
    return reader.isStartElement();
  }

  @Override
  public boolean isWhiteSpace() {
    return reader.isWhiteSpace();
  }

  @Override
  public int next() throws XMLStreamException {
    return reader.next();
  }

  @Override
  public int nextTag() throws XMLStreamException {
    return reader.nextTag();
  }

  @Override
  public void require(int arg0, String arg1, String arg2) throws XMLStreamException {
    reader.require(arg0, arg1, arg2);
  }

  @Override
  public boolean standaloneSet() {
    return reader.standaloneSet();
  }

  public XMLStreamReader getDelegateReader() {
    return reader;
  }

}
