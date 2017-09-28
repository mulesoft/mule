/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.editors;

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

/**
 * This handles qname{....} syntax as used in stockquote-soap-config.xml
 */
public class QNamePropertyEditor extends PropertyEditorSupport {

  private boolean explicit = false;

  public QNamePropertyEditor() {
    super();
  }

  public QNamePropertyEditor(boolean explicit) {
    this();
    this.explicit = explicit;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {

    if (text.startsWith("qname{")) {
      setValue(parseQName(text.substring(6, text.length() - 1)));
    } else if (!explicit) {
      setValue(parseQName(text));
    } else {
      setValue(new QName(text));
    }
  }

  protected QName parseQName(String val) {
    StringTokenizer st = new StringTokenizer(val, ":");
    List<String> elements = new ArrayList<String>();

    while (st.hasMoreTokens()) {
      elements.add(st.nextToken());
    }

    switch (elements.size()) {
      case 0: {
        return null;
      }
      case 1: {
        return new QName(elements.get(0));
      }
      case 2: {
        return new QName(elements.get(0), elements.get(1));
      }
      case 3: {
        return new QName(elements.get(1) + ":" + elements.get(2), elements.get(0));
      }
      default: {
        String prefix = elements.get(0);
        String local = elements.get(1);
        // namespace can have multiple colons in it, so just assume the rest
        // is a namespace
        String ns = val.substring(prefix.length() + local.length() + 2);
        return new QName(ns, local, prefix);
      }
    }
  }

  public static QName convert(String value) {
    QNamePropertyEditor editor = new QNamePropertyEditor();
    editor.setAsText(value);
    return (QName) editor.getValue();
  }

}
