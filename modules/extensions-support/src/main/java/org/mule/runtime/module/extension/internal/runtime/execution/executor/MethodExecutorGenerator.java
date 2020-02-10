/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.executor;

import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static net.bytebuddy.description.modifier.FieldManifestation.FINAL;
import static net.bytebuddy.description.modifier.Visibility.PRIVATE;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static net.bytebuddy.description.type.TypeDescription.Generic.Builder.parameterizedType;
import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION;
import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.NO_CONSTRUCTORS;
import static net.bytebuddy.implementation.bytecode.member.FieldAccess.forField;
import static net.bytebuddy.implementation.bytecode.member.MethodReturn.VOID;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.FileUtils.TEMP_DIR;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.ArgumentResolverDelegate;
import org.mule.runtime.module.extension.internal.runtime.execution.GeneratedClass;
import org.mule.runtime.module.extension.internal.runtime.execution.GeneratedInstance;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription.ForLoadedType;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodCall.ArgumentLoader;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

public class MethodExecutorGenerator {

  private static final String TARGET_INSTANCE_FIELD_NAME = "__targetInstance";
  private final Map<String, GeneratedClass<MethodExecutor>> executorClasses = new HashMap<>();
  private final int instanceId = identityHashCode(this);

  public GeneratedInstance<MethodExecutor> generate(Object targetInstance,
                                                    Method method,
                                                    ArgumentResolverDelegate argumentResolverDelegate) throws Exception {

    GeneratedClass<MethodExecutor> generatedClass = getExecutorClass(method, targetInstance);
    List<Object> args = new ArrayList<>();
    args.add(targetInstance);
    args.addAll(asList(argumentResolverDelegate.getArgumentResolvers()));

    MethodExecutor instance = (MethodExecutor) generatedClass.getGeneratedClass().getConstructors()[0]
        .newInstance(args.toArray(new Object[args.size()]));

    return new GeneratedInstance<>(instance, generatedClass);
  }

  private GeneratedClass<MethodExecutor> getExecutorClass(Method method, Object targetInstance) {
    String generatorName = getGeneratorName(method);
    return executorClasses.computeIfAbsent(generatorName, key -> generateExecutorClass(key, method, targetInstance));

  }

  private GeneratedClass<MethodExecutor> generateExecutorClass(String generatorName, Method method, Object targetInstance) {
    DynamicType.Builder<Object> operationWrapperClassBuilder = new ByteBuddy()
        .subclass(Object.class, NO_CONSTRUCTORS)
        .implement(MethodExecutor.class)
        .name(generatorName)
        .defineField(TARGET_INSTANCE_FIELD_NAME, method.getDeclaringClass(), PRIVATE, FINAL);

    for (int i = 0; i < method.getParameterTypes().length; ++i) {
      operationWrapperClassBuilder = operationWrapperClassBuilder
          .defineField(getParameterFieldName(method.getParameters()[i]),
                       parameterizedType(ArgumentResolver.class, method.getParameterTypes()[i]).build(),
                       PRIVATE, FINAL);
    }

    Annotatable<Object> constructorDefinition = operationWrapperClassBuilder
        .defineConstructor(PUBLIC)
        .withParameter(method.getDeclaringClass(), TARGET_INSTANCE_FIELD_NAME);

    for (int i = 0; i < method.getParameterTypes().length; ++i) {
      constructorDefinition = constructorDefinition
          .withParameter(parameterizedType(ArgumentResolver.class, method.getParameterTypes()[i]).build(),
                         getParameterFieldName(method.getParameters()[i]));
    }

    final Unloaded<Object> byteBuddyMadeWrapper = constructorDefinition.intercept(new Implementation() {

      @Override
      public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
      }

      @Override
      public ByteCodeAppender appender(Target implementationTarget) {
        return (methodVisitor, instrumentationContext, instrumentedMethod) -> {
          List<StackManipulation> stackManipulationItems = new ArrayList<>();

          stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
          stackManipulationItems.add(MethodInvocation.invoke(new ForLoadedType(Object.class)
                                                                 .getDeclaredMethods()
                                                                 .filter(isConstructor().and(takesArguments(0)))
                                                                 .getOnly()));
          stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
          stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(1));
          stackManipulationItems.add(forField(implementationTarget
                                                  .getInstrumentedType()
                                                  .getDeclaredFields()
                                                  .filter(named(TARGET_INSTANCE_FIELD_NAME)).getOnly())
                                         .write());

          for (int i = 0; i < method.getParameterTypes().length; ++i) {
            stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
            stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(i + 2));
            stackManipulationItems.add(forField(implementationTarget
                                                    .getInstrumentedType()
                                                    .getDeclaredFields()
                                                    .filter(named(getParameterFieldName(method.getParameters()[i])))
                                                    .getOnly())
                                           .write());
          }

          stackManipulationItems.add(VOID);

          StackManipulation.Size size = new StackManipulation.Compound(stackManipulationItems)
              .apply(methodVisitor, instrumentationContext);
          return new ByteCodeAppender.Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        };
      }
    })
        .defineMethod("execute", Object.class, PUBLIC)
        .withParameter(ExecutionContext.class, "executionContext")
        .throwing(Exception.class)
        .intercept(MethodCall.invoke(method)
                       .onField(TARGET_INSTANCE_FIELD_NAME)
                       .with(getArgumentLoaders(method))
                       .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)

        ).make();

    final File file = new File(TEMP_DIR, generatorName + ".class");
    if (file.exists()) {
      file.delete();
    }

    try (FileOutputStream os = new FileOutputStream(file)) {
      os.write(byteBuddyMadeWrapper.getBytes());
      CompositeClassLoader executorClassLoader = new CompositeClassLoader(method.getDeclaringClass().getClassLoader(),
                                                                          getClass().getClassLoader());

      return new GeneratedClass<>((Class<MethodExecutor>) byteBuddyMadeWrapper.load(executorClassLoader, INJECTION).getLoaded(),
                                  file);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not generate MethodExecutor class"), e);
    }
  }

  private String getGeneratorName(Method method) {
    return method.getDeclaringClass().getName() + "$" + method.getName() + "$" + instanceId + "$MethodComponentExecutorWrapper";
  }

  private ArgumentLoader.Factory getArgumentLoaders(Method method) {
    return new ArgumentLoader.Factory() {

      @Override
      public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
      }

      @Override
      public ArgumentLoader.ArgumentProvider make(Implementation.Target implementationTarget) {
        MethodDescription.InDefinedShape resolveInvocation = new ForLoadedType(ArgumentResolver.class)
            .getDeclaredMethods()
            .filter(named("resolve"))
            .getOnly();

        return (instrumentedMethod, invokedMethod) -> {
          final int parameterCount = method.getParameterCount();
          List<ArgumentLoader> loaders = new ArrayList<>(parameterCount);
          for (int i = 0; i < parameterCount; i++) {
            Parameter parameter = method.getParameters()[i];
            Class<?> parameterType = parameter.getType();
            String parameterFieldName = getParameterFieldName(parameter);
            loaders.add((target, assigner, typing) -> {
              FieldDescription fieldDescription = instrumentedMethod.getDeclaringType()
                  .getDeclaredFields()
                  .filter(named(parameterFieldName))
                  .getOnly();

              List<StackManipulation> stack = new LinkedList<>();
              stack.add(MethodVariableAccess.loadThis());
              stack.add(FieldAccess.forField(fieldDescription).read());
              stack.add(MethodVariableAccess.REFERENCE.loadFrom(1));
              stack.add(MethodInvocation.invoke(resolveInvocation));
              stack.add(assigner.assign(new ForLoadedType(Object.class).asGenericType(),
                                        new ForLoadedType(parameterType).asGenericType(), typing));

              return new StackManipulation.Compound(stack);
            });
          }

          return loaders;
        };
      }
    };
  }

  private String getParameterFieldName(Parameter parameter) {
    return parameter.getName() + "Resolver";
  }
}
