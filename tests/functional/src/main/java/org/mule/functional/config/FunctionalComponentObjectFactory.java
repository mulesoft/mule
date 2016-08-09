/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.config;

import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationCallback;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.component.DefaultJavaComponent;

/**
 * {@link ObjectFactory} for functional test component
 *
 * @since 4.0
 */
public class FunctionalComponentObjectFactory implements ObjectFactory<MessageProcessor> {

  private FunctionalTestComponent component = newComponentInstance();

  protected FunctionalTestComponent newComponentInstance() {
    return new FunctionalTestComponent();
  }

  @Override
  public MessageProcessor getObject() throws Exception {
    return new DefaultJavaComponent(getFunctionalComponentObjectFactory());
  }

  private org.mule.runtime.core.api.object.ObjectFactory getFunctionalComponentObjectFactory() {
    return new org.mule.runtime.core.api.object.ObjectFactory() {

      @Override
      public Object getInstance(MuleContext muleContext) throws Exception {
        return component;
      }

      @Override
      public Class<?> getObjectClass() {
        return component.getClass();
      }

      @Override
      public boolean isSingleton() {
        return true;
      }

      @Override
      public boolean isExternallyManagedLifecycle() {
        return false;
      }

      @Override
      public boolean isAutoWireObject() {
        return false;
      }

      @Override
      public void addObjectInitialisationCallback(InitialisationCallback callback) {}

      @Override
      public void dispose() {}

      @Override
      public void initialise() throws InitialisationException {}
    };
  }

  public void setEventCallback(EventCallback eventCallback) {
    component.setEventCallback(eventCallback);
  }

  public void setReturnData(Object returnData) {
    component.setReturnData(returnData);
  }

  public void setThrowException(boolean throwException) {
    component.setThrowException(throwException);
  }

  public void setExceptionToThrow(Class<? extends Throwable> exceptionToThrow) {
    component.setExceptionToThrow(exceptionToThrow);
  }

  public void setExceptionText(String exceptionText) {
    component.setExceptionText(exceptionText);
  }

  public void setEnableMessageHistory(boolean enableMessageHistory) {
    component.setEnableMessageHistory(enableMessageHistory);
  }

  public void setEnableNotifications(boolean enableNotifications) {
    component.setEnableNotifications(enableNotifications);
  }

  public void setDoInboundTransform(boolean doInboundTransform) {
    component.setDoInboundTransform(doInboundTransform);
  }

  public void setAppendString(String appendString) {
    component.setAppendString(appendString);
  }

  public void setWaitTime(long waitTime) {
    component.setWaitTime(waitTime);
  }

  public void setLogMessageDetails(boolean logMessageDetails) {
    component.setLogMessageDetails(logMessageDetails);
  }

  public void setId(String id) {
    component.setId(id);
  }

  public void setMuleContext(MuleContext muleContext) {
    component.setMuleContext(muleContext);
  }
}
