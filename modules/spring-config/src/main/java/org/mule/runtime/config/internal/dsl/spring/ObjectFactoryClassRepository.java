/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.springframework.cglib.proxy.Enhancer.registerStaticCallbacks;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectTypeProvider;

import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Repository for storing the dynamic class generated to mimic {@link org.springframework.beans.factory.FactoryBean} from an
 * {@link ObjectFactory}. This is done because we need dependency injection and instrospection done over the {@link ObjectFactory}
 * without the user knowing about it.
 * <p>
 * The created {@link org.springframework.beans.factory.FactoryBean} is the one that receives the injection of fields declared by
 * the {@link ObjectFactory}. It also provides information about the instance type that is creating since it's used to know the
 * order in which beans must be initialised based on the dependencies between them.
 * <p>
 * The repository has a cache of the created classes based on the configuration of the {@link ComponentBuildingDefinition} and the
 * component configuration.
 *
 * @since 4.0
 */
public class ObjectFactoryClassRepository {

  private Cache<ComponentBuildingDefinition, Class<ObjectFactory>> objectFactoryClassCache = CacheBuilder.newBuilder()
      .removalListener(notification -> registerStaticCallbacks((Class<ObjectFactory>) notification.getValue(), null)).build();

  /**
   * Retrieves a {@link Class} for the {@link ObjectFactory} defined by the {@code objectFactoryType} parameter. Once acquired the
   * {@code Class} instance should not be reused for another {@link ComponentBuildingDefinition}.
   *
   * @param componentBuildingDefinition the definition on how to build the component
   * @param objectFactoryType the {@link ObjectFactory} of the component
   * @param createdObjectType the type of object created by the {@code ObjectFactory}
   * @param isLazyInitFunction function that defines if the object created by the component can be created lazily
   * @param instancePostCreationFunction function to do custom processing of the created instance by the {@code ObjectFactory}.
   *        When there's no need for post processing this value must be {@link Optional#empty()}
   * @return the {@code FactoryBean} class to be used by spring for the provided configuration.
   */
  public Class<ObjectFactory> getObjectFactoryClass(ComponentBuildingDefinition componentBuildingDefinition,
                                                    Class objectFactoryType,
                                                    Class createdObjectType,
                                                    Supplier<Boolean> isLazyInitFunction,
                                                    Consumer<Object> instancePostCreationFunction) {
    try {
      return objectFactoryClassCache.get(componentBuildingDefinition,
                                         () -> getObjectFactoryDynamicClass(componentBuildingDefinition,
                                                                            objectFactoryType,
                                                                            createdObjectType, isLazyInitFunction,
                                                                            instancePostCreationFunction));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Class<ObjectFactory> getObjectFactoryDynamicClass(final ComponentBuildingDefinition componentBuildingDefinition,
                                                            final Class objectFactoryType,
                                                            final Class createdObjectType,
                                                            final Supplier<Boolean> isLazyInitFunction,
                                                            final Consumer<Object> instancePostCreationFunction) {
    /*
     * We need this to allow spring create the object using a FactoryBean but using the object factory setters and getters so we
     * create as FactoryBean a dynamic class that will have the same attributes and methods as the ObjectFactory that the user
     * defined. This way our API does not expose spring specific classes.
     */
    Enhancer enhancer = new Enhancer();
    // Use SmartFactoryBean since it's the only way to force spring to pre-instantiate FactoryBean for singletons
    enhancer.setInterfaces(new Class[] {SmartFactoryBean.class});
    enhancer.setSuperclass(objectFactoryType);
    enhancer.setCallbackType(MethodInterceptor.class);
    if (SmartFactoryBean.class.getClassLoader() != objectFactoryType.getClassLoader()) {
      // CGLIB needs access to both the spring interface and the extended factory class.
      // If the factory class is defined in a plugin, its classloader has to be passed.
      enhancer.setClassLoader(new CompositeClassLoader(ObjectFactoryClassRepository.class.getClassLoader(),
                                                       objectFactoryType.getClassLoader()));
    }
    enhancer.setUseCache(false);
    Class<ObjectFactory> factoryBeanClass = enhancer.createClass();
    registerStaticCallbacks(factoryBeanClass, new Callback[] {
        (MethodInterceptor) (obj, method, args, proxy) -> {
          if (method.getName().equals("isSingleton")) {
            return !componentBuildingDefinition.isPrototype();
          }
          if (method.getName().equals("getObjectType") && !ObjectTypeProvider.class.isAssignableFrom(obj.getClass())) {
            return createdObjectType;
          }
          if (method.getName().equals("getObject")) {
            Object createdInstance = proxy.invokeSuper(obj, args);
            instancePostCreationFunction.accept(createdInstance);
            return createdInstance;
          }
          if (method.getName().equals("isPrototype")) {
            return componentBuildingDefinition.isPrototype();
          }
          if (method.getName().equals("isEagerInit")) {
            return !isLazyInitFunction.get();
          }
          return proxy.invokeSuper(obj, args);
        }
    });
    return factoryBeanClass;
  }

  /**
   * Removes all registered callbacks create for each created {@code FactoryBean} class. This is a must since it prevents a memory
   * leak in CGLIB
   */
  public void destroy() {
    objectFactoryClassCache.invalidateAll();
  }

}
