/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.config;

import static java.util.ServiceLoader.load;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;
import org.mule.weave.v2.el.metadata.WeaveExpressionLanguageMetadataServiceImpl;

import java.util.Iterator;

/**
 * Provides a way to override how the {@link DefaultExpressionLanguageFactoryService} for DataWeave to use in tests is obtained.
 * 
 * @since 4.5
 */
public interface WeaveExpressionLanguageFactoryServiceProvider {

  public static DefaultExpressionLanguageFactoryService provideDefaultExpressionLanguageFactoryService() {
    final Iterator<WeaveExpressionLanguageFactoryServiceProvider> iterator =
        load(WeaveExpressionLanguageFactoryServiceProvider.class,
             WeaveExpressionLanguageFactoryServiceProvider.class.getClassLoader()).iterator();

    if (iterator.hasNext()) {
      return iterator.next().createDefaultExpressionLanguageFactoryService();
    } else {
      return new WeaveDefaultExpressionLanguageFactoryService(null);
    }
  }

  public static ExpressionLanguageMetadataService provideExpressionLanguageMetadataService() {
    final Iterator<WeaveExpressionLanguageFactoryServiceProvider> iterator =
        load(WeaveExpressionLanguageFactoryServiceProvider.class,
             WeaveExpressionLanguageFactoryServiceProvider.class.getClassLoader()).iterator();

    if (iterator.hasNext()) {
      return iterator.next().createExpressionLanguageMetadataService();
    } else {
      return new WeaveExpressionLanguageMetadataServiceImpl();
    }
  }

  DefaultExpressionLanguageFactoryService createDefaultExpressionLanguageFactoryService();

  ExpressionLanguageMetadataService createExpressionLanguageMetadataService();

}
