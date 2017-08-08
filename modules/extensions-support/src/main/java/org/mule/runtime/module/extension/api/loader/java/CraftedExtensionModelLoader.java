/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

/**
 * {@link ExtensionModelLoader} implementation to be used for extensions which model is {@code hand crafted}.
 * <p>
 * It requires a property of name {@link #TYPE_PROPERTY_NAME} to be specified. Such property should contain
 * the {@code fqn} of a class which implements {@link ExtensionLoadingDelegate}. Such implementation should be
 * concrete and contain a public default constructor.
 *
 * @see ExtensionLoadingDelegate
 * @since 4.0
 */
public class CraftedExtensionModelLoader extends ExtensionModelLoader {

  public static final String CRAFTED_LOADER_ID = "crafted";
  public static final String TYPE_PROPERTY_NAME = "type";

  @Override
  public String getId() {
    return CRAFTED_LOADER_ID;
  }

  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    Class<?> delegateType = getDelegateType(context, context.getExtensionClassLoader());

    if (!ExtensionLoadingDelegate.class.isAssignableFrom(delegateType)) {
      throw new IllegalArgumentException(format(
                                                "Property '%s' was expected to point to an implementation of the '%s', but '%s' was found instead",
                                                TYPE_PROPERTY_NAME, ExtensionLoadingDelegate.class.getName(),
                                                delegateType.getClass().getName()));
    }

    if (!isInstantiable(delegateType)) {
      throw new IllegalArgumentException(format(
                                                "Type '%s' is not instantiable. A concrete class with a public default constructor was expected",
                                                delegateType.getName()));
    }

    ExtensionLoadingDelegate delegate;
    try {
      delegate = (ExtensionLoadingDelegate) ClassUtils.instantiateClass(delegateType);
    } catch (Throwable e) {
      throw new MuleRuntimeException(createStaticMessage(format("Could not instantiate type '%s'", delegateType.getName())), e);
    }

    delegate.accept(context.getExtensionDeclarer(), context);
  }

  private Class<?> getDelegateType(ExtensionLoadingContext context, ClassLoader classLoader) {
    String type = context.<String>getParameter(TYPE_PROPERTY_NAME)
        .orElseThrow(() -> new IllegalArgumentException(format("Property '%s' has not been specified", TYPE_PROPERTY_NAME)));

    if (isBlank(type)) {
      throw new IllegalArgumentException(format("'%s' property cannot be blank", TYPE_PROPERTY_NAME));
    }

    try {
      return loadClass(type, classLoader);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(format("Class '%s' cannot be loaded", type), e);
    }
  }
}
