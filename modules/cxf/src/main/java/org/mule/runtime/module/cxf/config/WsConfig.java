/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.config;

import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.util.AttributeEvaluator;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class WsConfig implements MuleContextAware {

  Map<String, Object> configProperties = new HashMap<>();

  private MuleContext muleContext;

  public WsConfig() {

  }

  public WsConfig(Map<String, Object> configProperties) {
    this.configProperties = configProperties;
  }

  public void setConfigProperties(Map<String, Object> configProperties) {
    this.configProperties = configProperties;
  }

  public Map<String, Object> getConfigProperties() {
    return new DynamicAttributeEvalutorMap(new HashMap<>(configProperties), muleContext);
  }

  private static class DynamicAttributeEvalutorMap implements Map<String, Object> {

    private Map<String, Object> map;
    private MuleContext muleContext;

    public DynamicAttributeEvalutorMap(Map<String, Object> map, MuleContext muleContext) {
      this.map = map;
      this.muleContext = muleContext;
    }

    private Map<String, Object> getEvaluatedMap() {
      MuleEvent event = RequestContext.getEvent();
      MuleMessage message = (event != null) ? event.getMessage() : null;
      if (map != null && message != null) {
        Map<String, Object> evaluatedMap = new LinkedHashMap<>(map.size());
        for (Entry<String, Object> entry : map.entrySet()) {
          if (entry.getValue() instanceof String) {
            AttributeEvaluator evaluator = new AttributeEvaluator((String) entry.getValue());
            evaluator.initialize(muleContext.getExpressionManager());
            evaluatedMap.put(entry.getKey(), evaluator.resolveValue(event));
          } else {
            evaluatedMap.put(entry.getKey(), entry.getValue());
          }
        }
        return evaluatedMap;
      }
      return map;
    }

    @Override
    public void clear() {
      map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
      return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return map.containsValue(value);
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
      return getEvaluatedMap().entrySet();
    }

    @Override
    public Object get(Object key) {
      return getEvaluatedMap().get(key);
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public Set<String> keySet() {
      return map.keySet();
    }

    @Override
    public Object put(String key, Object value) {
      return map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
      map.putAll(m);
    }

    @Override
    public Object remove(Object key) {
      return map.remove(key);
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public Collection<Object> values() {
      return getEvaluatedMap().values();
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
