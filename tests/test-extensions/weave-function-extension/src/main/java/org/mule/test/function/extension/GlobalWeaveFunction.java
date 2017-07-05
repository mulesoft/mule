/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.function.extension;

import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.Optional;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class GlobalWeaveFunction implements Initialisable {

  private XPathFactory xPathFactory;

  @Override
  public void initialise() throws InitialisationException {
    if (xPathFactory == null) {
      xPathFactory = XPathFactory.newInstance();
    }
  }

  public String customEcho(String echo) {
    return echo;
  }

  public Object echoWithDefault(@Optional(defaultValue = PAYLOAD) Object payload,
                                @Optional(defaultValue = "prefix_") String prefix) {
    return prefix.concat(String.valueOf(payload));
  }

  public Map<String, String> toMap(SimplePojo pojo) {
    return ImmutableMap.of("user", pojo.getUser(), "pass", pojo.getPass());
  }

  public Object xpath(String expression,
                      @Optional(defaultValue = PAYLOAD) Object item,
                      @Optional String returnType) {
    try {
      if (returnType == null) {
        return xPathFactory.newXPath().evaluate(expression, item);
      } else {
        return xPathFactory.newXPath().evaluate(expression, item, asQname(returnType));
      }
    } catch (XPathExpressionException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private QName asQname(String name) {
    return new QName("http://www.w3.org/1999/XSL/Transform", name.toUpperCase());
  }

}
