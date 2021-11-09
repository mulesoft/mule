/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transformer;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;
import static org.mule.runtime.core.privileged.util.BeanUtils.getName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getParameterClasses;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSubtypeClasses;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.transformer.simple.StringToEnum;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

/**
 * Registers the transformer for the enums of each registered extension.
 * 
 * @since 4.5
 */
public class EnumTransformersRegistry implements Initialisable {

  @Inject
  private MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    Set<Class<? extends Enum>> enumTypes = new HashSet<>();

    muleContext.getExtensionManager().getExtensions().forEach(ext -> {
      registerEnumTransformers(ext, enumTypes);
    });
  }

  private void registerEnumTransformers(ExtensionModel extensionModel, Set<Class<? extends Enum>> enumTypes) {
    ClassLoader classLoader = getClassLoader(extensionModel);
    Set<Class<?>> parameterClasses = new HashSet<>();

    parameterClasses.addAll(getParameterClasses(extensionModel, classLoader));
    parameterClasses.addAll(getSubtypeClasses(extensionModel, classLoader));

    parameterClasses.stream()
        .filter(type -> Enum.class.isAssignableFrom(type))
        .forEach(type -> {
          final Class<Enum> enumClass = (Class<Enum>) type;
          if (enumTypes.add(enumClass)) {
            try {
              StringToEnum stringToEnum = new StringToEnum(enumClass);
              registerObject(muleContext, getName(stringToEnum), stringToEnum, Transformer.class);
            } catch (MuleException e) {
              throw new MuleRuntimeException(createStaticMessage("Could not register transformer for enum "
                  + enumClass.getName()), e);
            }
          }
        });
  }

}
