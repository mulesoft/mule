/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml;

import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider.maskPasswords;

import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider;

/**
 * Internal object responsible to traverse the entire XMl of a given application to read the {@link ComponentModel} to
 * recreate the macro expanded XML of the application.
 *
 * It will obfuscate attributes that matches with {@link LocationExecutionContextProvider#maskPasswords(String, String)}.
 * @since 4.2.0
 */
class ComponentModelReaderHelper {

  static final String PASSWORD_MASK = "@@credentials@@";

  private ComponentModelReaderHelper() {}

  static String toXml(ComponentModel rootComponentModel) {
    return toXml(rootComponentModel, 0);
  }

  static private String toXml(ComponentModel cm, int tab) {
    final String spaces = repeat(" ", tab * 3);
    final StringBuilder sb = new StringBuilder(spaces).append("<").append(cm.getIdentifier().toString());
    cm.getRawParameters().forEach((id, value) -> sb.append(" ").append(id).append("=\"").append(value).append("\""));
    if (cm.getInnerComponents().isEmpty() && isBlank(cm.getTextContent())) {
      sb.append("/>");
    } else {
      sb.append(">");
      cm.getInnerComponents()
          .forEach(componentModel -> sb.append(lineSeparator()).append(toXml(componentModel, tab + 1)));
      if (isNotBlank(cm.getTextContent())) {
        sb.append("<![CDATA[").append(spaces).append(cm.getTextContent()).append("]]>");
      }
      sb.append(lineSeparator()).append(spaces).append("</").append(cm.getIdentifier().toString()).append(">");
    }
    return maskPasswords(sb.toString(), PASSWORD_MASK);
  }

}
