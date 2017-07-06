/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.extension.api.client.ExtensionsClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.TransactionManager;

public class DependencyInjectionObject {

  @Inject
  private ConfigurationComponentLocator configurationComponentLocator;

  @Inject
  @Named("_muleLocalObjectStoreManager")
  private ObjectStoreManager localObjectStoreManager;

  @Inject
  @Named("_muleObjectStoreManager")
  private ObjectStoreManager objectStoreManager;

  @Inject
  @Named("_muleLocalLockFactory")
  private LockFactory localLockFactory;

  @Inject
  @Named("_muleLockFactory")
  private LockFactory lockFactory;

  @Inject
  private ExpressionLanguage expressionLanguage;

  @Inject
  private MuleExpressionLanguage muleExpressionLanguage;

  @Inject
  private ExtensionsClient extensionsClient;

  @Inject
  private TransformationService transformationService;

  @Inject
  private ObjectSerializer objectSerializer;

  @Inject
  private ServerNotificationHandler serverNotificationHandler;

  public ConfigurationComponentLocator getConfigurationComponentLocator() {
    return configurationComponentLocator;
  }

  public ObjectStoreManager getLocalObjectStoreManager() {
    return localObjectStoreManager;
  }

  public ObjectStoreManager getObjectStoreManager() {
    return objectStoreManager;
  }

  public LockFactory getLocalLockFactory() {
    return localLockFactory;
  }

  public LockFactory getLockFactory() {
    return lockFactory;
  }

  public ExpressionLanguage getExpressionLanguage() {
    return expressionLanguage;
  }

  public MuleExpressionLanguage getMuleExpressionLanguage() {
    return muleExpressionLanguage;
  }

  public ExtensionsClient getExtensionsClient() {
    return extensionsClient;
  }

  public TransformationService getTransformationService() {
    return transformationService;
  }

  public ObjectSerializer getObjectSerializer() {
    return objectSerializer;
  }

  public ServerNotificationHandler getServerNotificationHandler() {
    return serverNotificationHandler;
  }

}
