/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformer;

import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.module.xml.util.NamespaceManager;
import org.mule.runtime.module.xml.util.XMLUtils;
import org.mule.runtime.module.xml.xpath.SaxonXpathEvaluator;
import org.mule.runtime.module.xml.xpath.XPathEvaluator;
import org.mule.runtime.module.xml.xpath.XPathReturnType;

import java.nio.charset.Charset;
import java.util.Map;

import org.xml.sax.InputSource;

/**
 * Simple transformer for using the JAXP XPath library to extract an XPath value from an XPath expression.
 *
 * @author Ryan Heaton
 */
public class XPathExtractor extends AbstractTransformer implements MuleContextAware {

  private XPathEvaluator xpathEvaluator;
  private volatile Map<String, String> prefixToNamespaceMap = null;
  private volatile String expression;
  private volatile XPathReturnType resultType = XPathReturnType.STRING;
  private NamespaceManager namespaceManager;

  public XPathExtractor() {
    registerSourceType(DataType.fromType(org.w3c.dom.Node.class));
    registerSourceType(DataType.fromType(InputSource.class));
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    try {
      namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
    } catch (RegistrationException e) {
      throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();

    if (expression == null) {
      throw new InitialisationException(
                                        MessageFactory
                                            .createStaticMessage("An expression must be supplied to the StandardXPathExtractor"),
                                        this);
    }

    if (xpathEvaluator == null) {
      xpathEvaluator = new SaxonXpathEvaluator();
    }

    if (namespaceManager != null) {
      xpathEvaluator.registerNamespaces(namespaceManager);
    }

    if (prefixToNamespaceMap != null) {
      xpathEvaluator.registerNamespaces(prefixToNamespaceMap);
    }
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    MuleEvent event = getCurrentEvent();
    try {
      return xpathEvaluator.evaluate(expression, XMLUtils.toDOMNode(src, event), resultType, event);
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }

  /**
   * @return Returns the expression.
   */
  public String getExpression() {
    return expression;
  }

  /**
   * @param expression The expression to set.
   */
  public void setExpression(String expression) {
    this.expression = expression;
  }

  /**
   * Result type from this transformer.
   *
   * @return Result type from this transformer.
   */
  public XPathReturnType getResultType() {
    return resultType;
  }

  /**
   * Result type from this transformer.
   *
   * @param resultType Result type from this transformer.
   */
  public void setResultType(XPathReturnType resultType) {
    this.resultType = resultType;
  }

  public XPathEvaluator getXpathEvaluator() {
    return xpathEvaluator;
  }

  public void setXpathEvaluator(XPathEvaluator xpathEvaluator) {
    this.xpathEvaluator = xpathEvaluator;
  }

  /**
   * The prefix-to-namespace map.
   *
   * @return The prefix-to-namespace map.
   */
  public Map<String, String> getNamespaces() {
    return prefixToNamespaceMap;
  }

  /**
   * The prefix-to-namespace map.
   *
   * @param prefixToNamespaceMap The prefix-to-namespace map.
   */
  public void setNamespaces(Map<String, String> prefixToNamespaceMap) {
    this.prefixToNamespaceMap = prefixToNamespaceMap;
  }
}
