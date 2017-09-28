/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.function.extension;

import static com.google.common.collect.Lists.partition;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.w3c.dom.Document;

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

  public String defaultPrimitives(@Optional boolean bool, @Optional int number,
                                  @Optional double doubles, @Optional float floats,
                                  @Optional long longs, @Optional short shorts) {
    return "SUCCESS";
  }

  public Object echoWithDefault(@Optional(defaultValue = PAYLOAD) Object payload,
                                @Optional(defaultValue = "prefix_") String prefix) {
    return prefix.concat(String.valueOf(payload));
  }

  public Map<String, String> toMap(SimplePojo pojo) {
    return ImmutableMap.of("user", pojo.getUser(), "pass", pojo.getPass());
  }

  public Object xpath(String expression,
                      @Optional(defaultValue = PAYLOAD) InputStream item,
                      @Optional String returnType) {
    try {

      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(item);
      if (returnType == null) {
        return xPathFactory.newXPath().evaluate(expression, document);
      } else {
        return xPathFactory.newXPath().evaluate(expression, document, asQname(returnType));
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Alias("partition")
  public List<List<Object>> aliasedFunction(List<Object> listToSplit, int groupSize) {
    return partition(listToSplit, groupSize);
  }

  private QName asQname(String name) {
    return new QName("http://www.w3.org/1999/XSL/Transform", name.toUpperCase());
  }

}
