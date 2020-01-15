/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_AUTH_CODE_HANDLER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_CLIENT_CREDENTIALS_HANDLER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_PLATFORM_MANAGED_HANDLER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLICY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials.ClientCredentialsOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs.PlatformManagedOAuthHandler;

import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;

/**
 * Populates a reference to a Mule API injectable object into an object.
 *
 * @since 4.0
 */
class ObjectReferencePopulator {

  // TODO MULE-9682: Load this list dynamically and define the whole set of objects that can be injected.
  private static ImmutableMap<Class<?>, String> OBJECT_REFERENCES =
      new ImmutableMap.Builder()
          .put(Registry.class, OBJECT_REGISTRY)
          .put(MuleContext.class, OBJECT_MULE_CONTEXT)
          .put(TimeSupplier.class, OBJECT_TIME_SUPPLIER)
          .put(ExtensionManager.class, OBJECT_EXTENSION_MANAGER)
          .put(PolicyManager.class, OBJECT_POLICY_MANAGER)
          .put(AuthorizationCodeOAuthHandler.class, OBJECT_EXTENSION_AUTH_CODE_HANDLER)
          .put(ClientCredentialsOAuthHandler.class, OBJECT_EXTENSION_CLIENT_CREDENTIALS_HANDLER)
          .put(PlatformManagedOAuthHandler.class, OBJECT_EXTENSION_PLATFORM_MANAGED_HANDLER)
          .build();


  /**
   * @param type the type of the object to be popoulated with
   * @param typeIdConsumer a {@code Consumer} that will be provided with the identifier of the object to populate
   */
  public void populate(Class<?> type, Consumer<String> typeIdConsumer) {
    String referenceName = OBJECT_REFERENCES.get(type);
    if (referenceName == null) {
      throw new MuleRuntimeException(createStaticMessage("Could not determine reference object of type: " + type.getName()));
    }
    typeIdConsumer.accept(referenceName);
  }

}
