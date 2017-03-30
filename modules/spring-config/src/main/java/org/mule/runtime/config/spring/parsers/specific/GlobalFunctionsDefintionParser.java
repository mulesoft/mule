/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import org.mule.runtime.config.spring.parsers.assembly.BeanAssembler;
import org.mule.runtime.config.spring.parsers.generic.TextDefinitionParser;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class GlobalFunctionsDefintionParser extends TextDefinitionParser {

  private static String FUNCTION_FILE_ATTRIBUTE_NAME = "file";

  public GlobalFunctionsDefintionParser(String setterMethod) {
    super(setterMethod);
  }

  @Override
  protected void postProcess(ParserContext context, BeanAssembler assembler, Element element) {
    super.postProcess(context, assembler, element);
    if (element.hasAttribute(FUNCTION_FILE_ATTRIBUTE_NAME)) {
      assembler.getTarget().getPropertyValues().add("globalFunctionsFile", element.getAttribute(FUNCTION_FILE_ATTRIBUTE_NAME));
    }
  }

  @Override
  protected void addPropertyValue(String aValue, MutablePropertyValues aTempPropertyValues) {
    PropertyValue tempCurrentPropertyValue = aTempPropertyValues.getPropertyValue(setterMethod);

    if (tempCurrentPropertyValue != null) {
      Object tempCurrentValue = tempCurrentPropertyValue.getValue();
      if (logger.isDebugEnabled()) {
        logger.debug("Concat " + setterMethod + " " + tempCurrentValue + " with " + aValue);
      }
      super.addPropertyValue(tempCurrentValue + "\n" + aValue, aTempPropertyValues);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Set " + setterMethod + " with " + aValue);
      }
      super.addPropertyValue(aValue, aTempPropertyValues);
    }
  }

}
