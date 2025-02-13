/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.IS_CDATA;

import static java.lang.Boolean.TRUE;

import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.MetadataTypeObjectFieldTypeAdapter;

/**
 * Bean definition creator for elements that end up representing simple types.
 * <p>
 * Elements that represent a simple type have the form {@code <element value="simpleValue"/>} or
 * {@code <element>simpleValue</element>}
 *
 * @since 4.4
 */
class SimpleTypeBeanParamDefinitionCreator extends SimpleTypeBeanBaseDefinitionCreator<CreateParamBeanDefinitionRequest> {

  private final boolean disableTrimWhitespaces;
  private final boolean disablePojoCdataTrimWhitespaces;

  public SimpleTypeBeanParamDefinitionCreator(boolean disableTrimWhitespaces, boolean disablePojoCdataTrimWhitespaces) {
    this.disableTrimWhitespaces = disableTrimWhitespaces;
    this.disablePojoCdataTrimWhitespaces = disablePojoCdataTrimWhitespaces;
  }

  @Override
  protected boolean doHandleRequest(CreateParamBeanDefinitionRequest createBeanDefinitionRequest, Class<?> type) {
    final ComponentParameterAst param = createBeanDefinitionRequest.getParam();
    this.setConvertibleBeanDefinition(createBeanDefinitionRequest, type,
                                      (String) resolveParamValue(param, disableTrimWhitespaces, disablePojoCdataTrimWhitespaces));
    return true;
  }

  static Object resolveParamValue(final ComponentParameterAst param, boolean disableTrimWhitespaces,
                                  boolean disablePojoCdataTrimWhitespaces) {
    return param.getValue()
        .mapLeft(expr -> DEFAULT_EXPRESSION_PREFIX + expr + DEFAULT_EXPRESSION_POSTFIX)
        .mapRight(value -> {
          if (value instanceof String
              && ((!disableTrimWhitespaces && !isCdata(param))
                  || (!disablePojoCdataTrimWhitespaces && isCdata(param)
                      && param.getModel() instanceof MetadataTypeObjectFieldTypeAdapter))) {
            return ((String) value).trim();
          } else {
            return value;
          }
        })
        .getValue()
        .orElse(null);
  }

  private static boolean isCdata(final ComponentParameterAst param) {
    return TRUE.equals(param.getMetadata().map(m -> m.getParserAttributes().get(IS_CDATA)).orElse(false));
  }

}
