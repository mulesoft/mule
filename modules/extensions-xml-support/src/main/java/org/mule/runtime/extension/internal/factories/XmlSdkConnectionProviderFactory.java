/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.factories;

import static org.mule.runtime.core.internal.util.MultiParentClassLoaderUtils.multiParentClassLoaderFor;

import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;

import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.CHILD_FIRST;
import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.IMITATE_SUPER_CLASS;
import static org.apache.commons.beanutils.BeanUtils.copyProperties;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;


public class XmlSdkConnectionProviderFactory implements ConnectionProviderFactory {

  private static final Pattern VARS_EXPRESSION_PATTERN = compile("^#\\[vars\\.(\\w+)\\]$");

  private final static Function<ComponentParameterAst, Optional<String>> PROPERTY_USAGE =
      p -> p.getRawValue() != null
          ? of(VARS_EXPRESSION_PATTERN.matcher(p.getRawValue()))
              .filter(Matcher::matches)
              .map(matcher -> matcher.group(1))
          : empty();

  private final ComponentAst innerConnectionProviderComponent;
  private final XmlSdkConfigurationFactory configurationFactory;

  private final Supplier<Class<? extends XmlSdkConnectionProviderWrapper>> connectionWrapperClass;

  public XmlSdkConnectionProviderFactory(ComponentAst innerConnectionProviderComponent,
                                         List<ParameterDeclaration> configParamDeclarations,
                                         List<ParameterDeclaration> connProviderParamDeclarations,
                                         XmlSdkConfigurationFactory configurationFactory) {
    this.innerConnectionProviderComponent = requireNonNull(innerConnectionProviderComponent);
    this.configurationFactory = configurationFactory;

    this.connectionWrapperClass =
        new LazyValue<>(() -> createConnectionProviderBeanClass(resolveConnectionProviderProperties(innerConnectionProviderComponent,
                                                                                                    configParamDeclarations,
                                                                                                    connProviderParamDeclarations)));
  }

  private Set<ParameterDeclaration> resolveConnectionProviderProperties(ComponentAst innerConnectionProviderComponent,
                                                                        List<ParameterDeclaration> configParamDeclarations,
                                                                        List<ParameterDeclaration> connProviderParamDeclarations) {
    final Set<ParameterDeclaration> params = new HashSet<>();

    // Filter config provider properties that are used in the inner connection providers
    final Set<String> connectionProviderProperties =
        innerConnectionProviderComponent.recursiveStream()
            .flatMap(c -> c.getParameters().stream())
            .map(PROPERTY_USAGE)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toSet());

    configParamDeclarations
        .stream()
        .filter(configParamDecl -> connectionProviderProperties.contains(configParamDecl.getName()))
        .forEach(params::add);

    // Make sure that if property names collide, the one from the connection provider is used.
    params.addAll(connProviderParamDeclarations);
    return params;
  }

  private Class<? extends XmlSdkConnectionProviderWrapper> createConnectionProviderBeanClass(Collection<ParameterDeclaration> paramDeclarations) {
    DynamicType.Builder connectionProviderWrapperClassBuilder = new ByteBuddy()
        .subclass(XmlSdkConnectionProviderWrapper.class, IMITATE_SUPER_CLASS);

    for (ParameterDeclaration parameterDeclaration : paramDeclarations) {
      connectionProviderWrapperClassBuilder =
          connectionProviderWrapperClassBuilder.defineProperty(parameterDeclaration.getName(), String.class);
    }

    return connectionProviderWrapperClassBuilder.make().load(multiParentClassLoaderFor(currentThread().getContextClassLoader()),
                                                             CHILD_FIRST)
        .getLoaded();
  }

  @Override
  public ConnectionProvider newInstance() {
    // This will, instead, need to generate a dynamic class with the setters for the connection parameters of the extension, and
    // the impl of that generated class will have to forward them to the proper parameter in the delegate.

    try {
      final XmlSdkConnectionProviderWrapper connectionProviderWrapper =
          this.connectionWrapperClass.get().getDeclaredConstructor(ComponentAst.class, Function.class)
              .newInstance(innerConnectionProviderComponent, PROPERTY_USAGE);

      // This is a hack
      // In its current state, XML-SDK allows to declare parameters of the connection provider at the config level.
      // This forces code intended to work with just the connectionProvider to have knowledge of the parameters of the config.
      final Object lastConfig = configurationFactory.getLastBuilt();
      copyProperties(connectionProviderWrapper, lastConfig);
      return connectionProviderWrapper;
    } catch (IllegalAccessException | InvocationTargetException | InstantiationException | IllegalArgumentException
        | NoSuchMethodException | SecurityException e) {
      throw new MuleRuntimeException(e);
    }

  }

  @Override
  public Class getObjectType() {
    return this.connectionWrapperClass.get();
  }

}
