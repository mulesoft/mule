/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.object;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.InitialisationCallback;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.privileged.util.BeanUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates object instances based on the class and sets any properties. This factory is also responsible for applying any object
 * processors on the object before the lifecycle callbacks are called.
 */
public abstract class AbstractObjectFactory extends AbstractComponent implements ObjectFactory {

  protected String objectClassName;
  protected Class<?> objectClass;
  protected Map properties = null;
  protected List<InitialisationCallback> initialisationCallbacks = new ArrayList<>();
  protected FlowConstruct flowConstruct;
  protected boolean disposed = false;

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * For Spring only
   */
  public AbstractObjectFactory() {
    // nop
  }

  public AbstractObjectFactory(String objectClassName) {
    this(objectClassName, null);
  }

  public AbstractObjectFactory(String objectClassName, Map properties) {
    super();
    this.objectClassName = objectClassName;
    this.properties = properties;
    setupObjectClassFromObjectClassName();
  }

  public AbstractObjectFactory(Class<?> objectClass) {
    this(objectClass, null);
  }

  public AbstractObjectFactory(Class<?> objectClass, Map properties) {
    super();
    this.objectClassName = objectClass.getName();
    this.objectClass = objectClass;
    this.properties = properties;
  }

  protected Class<?> setupObjectClassFromObjectClassName() {
    try {
      Class<?> klass = org.apache.commons.lang3.ClassUtils.getClass(objectClassName);
      objectClass = klass;
      return klass;
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    if ((objectClassName == null) || (objectClass == null)) {
      throw new InitialisationException(I18nMessageFactory.createStaticMessage("Object factory has not been initialized."), this);
    }
    disposed = false;
  }

  @Override
  public void dispose() {
    disposed = true;
    // Don't reset the component config state i.e. objectClass since service objects can be recycled
  }

  /**
   * Creates an initialized object instance based on the class and sets any properties. This method handles all injection of
   * properties for the resulting object
   *
   * @param muleContext the current {@link org.mule.runtime.core.api.MuleContext} instance. This can be used for performing
   *        registry lookups applying processors to newly created objects or even firing custom notifications
   * @throws Exception Can throw any type of exception while creating a new object
   */
  @Override
  public Object getInstance(MuleContext muleContext) throws Exception {
    if (objectClass == null || disposed) {
      throw new InitialisationException(I18nMessageFactory.createStaticMessage("Object factory has not been initialized."), this);
    }

    Object object = ClassUtils.instantiateClass(objectClass);

    if (properties != null) {
      BeanUtils.populateWithoutFail(object, properties, true);
    }

    if (isAutoWireObject()) {
      ((MuleContextWithRegistries) muleContext).getRegistry().applyProcessors(object);
    }
    fireInitialisationCallbacks(object);

    return object;
  }

  protected void fireInitialisationCallbacks(Object component) throws InitialisationException {
    for (InitialisationCallback callback : initialisationCallbacks) {
      callback.initialise(component);
    }
  }

  @Override
  public Class<?> getObjectClass() {
    return objectClass;
  }

  public void setObjectClass(Class<?> objectClass) {
    this.objectClass = objectClass;
    this.objectClassName = objectClass.getName();
  }

  protected String getObjectClassName() {
    return objectClassName;
  }

  public void setObjectClassName(String objectClassName) {
    this.objectClassName = objectClassName;
    setupObjectClassFromObjectClassName();
  }

  protected Map getProperties() {
    return properties;
  }

  public void setProperties(Map properties) {
    this.properties = properties;
  }

  @Override
  public void addObjectInitialisationCallback(InitialisationCallback callback) {
    initialisationCallbacks.add(callback);
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  @Override
  public boolean isExternallyManagedLifecycle() {
    return false;
  }

  @Override
  public boolean isAutoWireObject() {
    return true;
  }
}
