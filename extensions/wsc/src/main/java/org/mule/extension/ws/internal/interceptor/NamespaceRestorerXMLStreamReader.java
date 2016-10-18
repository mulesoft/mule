/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.interceptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

/**
 * XMLStreamReader decorator that restores XML Namespace declarations, by default, it will restore the namespaces on the
 * first declaration, but this can be overridden
 *
 * @since 4.0
 */
public class NamespaceRestorerXMLStreamReader extends ScopeSaverXMLStreamReader {

  private List<Namespace> namespaces;
  private List<String> nsBlacklist = new ArrayList<>();

  public NamespaceRestorerXMLStreamReader(XMLStreamReader reader) {
    super(reader);
  }

  public final NamespaceRestorerXMLStreamReader blackList(String namespace) {
    nsBlacklist.add(namespace);
    return this;
  }

  public void restoreNamespaces() {
    if (getEventType() == START_ELEMENT) {
      namespaces = new ArrayList<>();

      Set<String> prefixes = new HashSet<>();
      for (StartElement elem : scopes()) {
        Iterator<Namespace> iterator = elem.getNamespaces();
        while (iterator.hasNext()) {
          Namespace ns = iterator.next();
          if (prefixes.add(ns.getPrefix()) && !nsBlacklist.contains(ns.getNamespaceURI())) {
            namespaces.add(ns);
          }
        }
      }
    }
  }

  @Override
  public int next() throws XMLStreamException {
    namespaces = null;
    return super.next();
  }

  @Override
  public int getNamespaceCount() {
    if (overrideNamespaces()) {
      return namespaces.size();
    }
    return super.getNamespaceCount();
  }

  private boolean overrideNamespaces() {
    return namespaces != null;
  }

  @Override
  public String getNamespacePrefix(int index) {
    if (overrideNamespaces()) {
      return namespaces.get(index).getPrefix();
    }
    return super.getNamespacePrefix(index);
  }

  @Override
  public String getNamespaceURI(String prefix) {
    if (overrideNamespaces()) {
      for (Namespace ns : namespaces) {
        if (ns.getPrefix().equals(prefix)) {
          return ns.getNamespaceURI();
        }
      }
    }
    return super.getNamespaceURI(prefix);
  }

  @Override
  public String getNamespaceURI(int index) {
    if (overrideNamespaces()) {
      return namespaces.get(index).getNamespaceURI();
    }
    return super.getNamespaceURI(index);
  }
}
