/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static net.sf.cglib.proxy.Enhancer.registerStaticCallbacks;

import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectTypeProvider;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.SmartFactoryBean;

import com.github.benmanes.caffeine.cache.LoadingCache;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * Repository for storing the dynamic class generated to mimic {@link org.springframework.beans.factory.FactoryBean} from an
 * {@link ObjectFactory}. This is done because we need dependency injection and instrospection done over the {@link ObjectFactory}
 * without the user knowing about it.
 * <p>
 * The created {@link org.springframework.beans.factory.FactoryBean} is the one that receives the injection of fields declared by
 * the {@link ObjectFactory}. It also provides information about the instance type that is creating since it's used to know the
 * order in which beans must be initialised based on the dependencies between them.
 *
 * @since 4.0
 */
public class ObjectFactoryClassRepository {

  //This only works because the cache uses an identity hashCode() and equals() for keys when they are configured as weak.
  //(check com.github.benmanes.caffeine.cache.Caffeine.weakKeys javadoc).
  //If that is not the case, this will never work because we want to compare class loaders by instance.
  //The idea for this cache is to avoid the creation of multiple CompositeClassLoader instances with the same delegates.
  //That is because CGLIB enhancer uses the composite class loader to define the enhanced class and every new instance loads
  //the same defined class over and over again, causing metaspace OOM in some scenarios.
  private static final LoadingCache<ClassLoader, ClassLoader> COMPOSITE_CL_CACHE = newBuilder()
      .weakKeys()
      .weakValues()
      .build(cl -> new CompositeClassLoader(ObjectFactoryClassRepository.class.getClassLoader(), cl));

  /**
   * Retrieves a {@link Class} for the {@link ObjectFactory} defined by the {@code objectFactoryType} parameter. Once acquired the
   * {@code Class} instance should not be reused for another {@link ComponentBuildingDefinition}.
   *
   * @param componentBuildingDefinition          the definition on how to build the component
   * @param objectFactoryType                    the {@link ObjectFactory} of the component
   * @param createdObjectType                    the type of object created by the {@code ObjectFactory}
   * @param isLazyInitFunction                   function that defines if the object created by the component can be created lazily
   * @param instancePostCreationFunctionOptional function to do custom processing of the created instance by the {@code ObjectFactory}.
   *                                             When there's no need for post processing this value must be {@link Optional#empty()}
   * @return the {@code FactoryBean} class to be used by spring for the provided configuration.
   */
  public Class<ObjectFactory> getObjectFactoryClass(ComponentBuildingDefinition componentBuildingDefinition,
                                                    Class objectFactoryType,
                                                    Class createdObjectType,
                                                    Supplier<Boolean> isLazyInitFunction) {
    return getObjectFactoryDynamicClass(componentBuildingDefinition, objectFactoryType, createdObjectType, isLazyInitFunction);
  }

  private Class<ObjectFactory> getObjectFactoryDynamicClass(final ComponentBuildingDefinition componentBuildingDefinition,
                                                            final Class objectFactoryType,
                                                            final Class createdObjectType,
                                                            final Supplier<Boolean> isLazyInitFunction) {
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
      enhancer.setClassLoader(COMPOSITE_CL_CACHE.get(objectFactoryType.getClassLoader()));
    }

    // The use of the CGLIB cache is turned off when a post creation function is passed as argument in order to
    // enrich the created proxy with properties. This is only to enable injecting properties in components
    // from the compatibility module.
    // Setting this to false will generate an excessive amount of different proxy classes loaded by the container CL
    // that will end up in Metaspace OOM.
    enhancer.setUseCache(true);

    // MG says: this is super important. Generating this variable here prevents the lambda below from
    // keeping a reference to the componentBuildingDefinition, which in turn references a classloader and has
    // caused leaks in the past
    final boolean prototype = componentBuildingDefinition.isPrototype();

    Class<ObjectFactory> factoryBeanClass = enhancer.createClass();
    registerStaticCallbacks(factoryBeanClass, new Callback[] {
        (MethodInterceptor) (obj, method, args, proxy) -> {
          final boolean eager = !isLazyInitFunction.get();

          if (method.getName().equals("isSingleton")) {
            return !prototype;
          }
          if (method.getName().equals("getObjectType") && !ObjectTypeProvider.class.isAssignableFrom(obj.getClass())) {
            return createdObjectType;
          }
          if (method.getName().equals("getObject")) {
            return proxy.invokeSuper(obj, args);
          }
          if (method.getName().equals("isPrototype")) {
            return prototype;
          }
          if (method.getName().equals("isEagerInit")) {
            return eager;
          }
          return proxy.invokeSuper(obj, args);
        }
    });
    return factoryBeanClass;
  }
}
