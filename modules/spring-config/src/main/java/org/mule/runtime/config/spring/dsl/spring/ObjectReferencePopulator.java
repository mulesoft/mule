/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.extension.api.ExtensionManager;

import com.google.common.collect.ImmutableMap;

import java.util.function.Consumer;

/**
 * Populates a reference to a Mule API injectable object into an object.
 *
 * @since 4.0
 */
class ObjectReferencePopulator {

  // TODO MULE-9682: Load this list dynamically and define the whole set of objects that can be injected.
  private static ImmutableMap<Class<?>, String> OBJECT_REFERENCES =
      new ImmutableMap.Builder().put(MuleContext.class, OBJECT_MULE_CONTEXT).put(TimeSupplier.class, OBJECT_TIME_SUPPLIER)
          .put(ExtensionManager.class, OBJECT_EXTENSION_MANAGER).build();


  /**
   * @param type the type of the object to be popoulated with
   * @param typeIdConsumer a {@code Consumer} that will be provided with the identifier of the object to populate
   */
  public void populate(Class<?> type, Consumer<String> typeIdConsumer) {
    String referenceName = OBJECT_REFERENCES.get(type);
    if (referenceName == null) {
      throw new MuleRuntimeException(CoreMessages
          .createStaticMessage("Could not determine reference object of type: " + type.getName()));
    }
    typeIdConsumer.accept(referenceName);
  }

}
