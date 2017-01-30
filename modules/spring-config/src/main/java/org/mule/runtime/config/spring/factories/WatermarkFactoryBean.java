/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.factories;

import static java.util.Optional.of;
import static org.mule.runtime.api.component.ComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.dsl.api.component.config.ComponentIdentifier.ANNOTATION_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ComponentLocation;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.source.polling.watermark.UpdateExpressionWatermark;
import org.mule.runtime.core.source.polling.watermark.Watermark;
import org.mule.runtime.core.source.polling.watermark.selector.SelectorWatermark;
import org.mule.runtime.core.source.polling.watermark.selector.WatermarkSelectorBroker;
import org.mule.runtime.core.util.store.MuleObjectStoreManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class WatermarkFactoryBean extends AbstractFactoryBean<Watermark> implements MuleContextAware, AnnotatedObject {

  public static final String MULE_WATERMARK_PARTITION = "mule.watermark";
  private static final String DEFAULT_SELECTOR_EXPRESSION = "#[mel:payload]";

  private String variable;
  private String defaultExpression;
  private String updateExpression;
  private WatermarkSelectorBroker selector;
  private String selectorExpression;
  private ObjectStore<Serializable> objectStore;

  private Map<QName, Object> annotations = new HashMap<>();
  private MuleContext muleContext;

  @Override
  public Class<?> getObjectType() {
    return Watermark.class;
  }

  @Override
  protected Watermark createInstance() throws Exception {
    if (this.selector != null) {
      if (!StringUtils.isEmpty(this.updateExpression)) {
        throw new IllegalArgumentException("You specified a watermark with both an update expression and a selector and/or a selector.\n"
            + "Those cannot co-exist. You have to either specify an updateExpression or selector options");
      }
      String selectorExpression =
          StringUtils.isEmpty(this.selectorExpression) ? DEFAULT_SELECTOR_EXPRESSION : this.selectorExpression;

      return new SelectorWatermark(this.acquireObjectStore(), this.variable, this.defaultExpression, this.selector,
                                   selectorExpression);
    } else {
      return new UpdateExpressionWatermark(this.acquireObjectStore(), this.variable, this.defaultExpression, updateExpression);
    }

  }

  private ObjectStore<Serializable> acquireObjectStore() {
    ObjectStore<Serializable> os = this.objectStore;
    if (os == null) {
      MuleObjectStoreManager mgr = (MuleObjectStoreManager) this.muleContext.getObjectStoreManager();
      os = mgr.getUserObjectStore(MULE_WATERMARK_PARTITION, true);
    }
    return os;
  }

  public void setObjectStore(ObjectStore<Serializable> objectStore) {
    this.objectStore = objectStore;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public void setDefaultExpression(String defaultExpression) {
    this.defaultExpression = defaultExpression;
  }

  public void setUpdateExpression(String updateExpression) {
    this.updateExpression = updateExpression;
  }

  public void setSelector(WatermarkSelectorBroker selector) {
    this.selector = selector;
  }

  public void setSelectorExpression(String selectorExpression) {
    this.selectorExpression = selectorExpression;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public Object getAnnotation(QName name) {
    return annotations.get(name);
  }

  @Override
  public Map<QName, Object> getAnnotations() {
    return annotations;
  }

  @Override
  public void setAnnotations(Map<QName, Object> annotations) {
    this.annotations = annotations;
  }

  @Override
  public ComponentIdentifier getIdentifier() {
    // TODO MULE-11572 set this data instead of building this object each time
    return new ComponentIdentifier() {

      @Override
      public String getNamespace() {
        return ((org.mule.runtime.dsl.api.component.config.ComponentIdentifier) getAnnotation(ANNOTATION_NAME)).getNamespace();
      }

      @Override
      public String getName() {
        return ((org.mule.runtime.dsl.api.component.config.ComponentIdentifier) getAnnotation(ANNOTATION_NAME)).getName();
      }

      @Override
      public ComponentType getComponentType() {
        return SOURCE;
      }
    };
  }

  @Override
  public ComponentLocation getLocation(String flowPath) {
    if (flowPath == null) {
      return null;
    } else {
      return new ComponentLocation() {

        @Override
        public String getPath() {
          return flowPath;
        }

        @Override
        public Optional<String> getFileName() {
          return of((String) getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName")));
        }

        @Override
        public Optional<Integer> getLineInFile() {
          return of((int) getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine")));
        }
      };
    }
  }
}
