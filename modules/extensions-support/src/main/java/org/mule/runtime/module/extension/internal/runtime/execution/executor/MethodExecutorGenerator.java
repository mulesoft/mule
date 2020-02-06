/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.executor;

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
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.FileUtils.TEMP_DIR;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.ArgumentResolverDelegate;
import org.mule.runtime.module.extension.internal.runtime.execution.ByteBuddyWrappedMethodComponentExecutor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

public class MethodExecutorGenerator {

  private static final String TARGET_INSTANCE_FIELD_NAME = "__targetInstance";
  private final Map<String, Class<MethodExecutor>> executorClasses = new HashMap<>();


  public MethodExecutor generate(Object targetInstance,
                                 Method method,
                                 ArgumentResolverDelegate argumentResolverDelegate)
      throws Exception {

    Class<MethodExecutor> executorClass = getExecutorClass(method);
    List<Object> args = new ArrayList<>();
    args.add(targetInstance);
    args.addAll(asList(argumentResolverDelegate.getArgumentResolvers()));

    return (MethodExecutor) executorClass.getConstructors()[0].newInstance(args.toArray(new Object[args.size()]));
  }

  private Class<MethodExecutor> getExecutorClass(Method method) {
    String generatorName = getGeneratorName(method);
    return executorClasses.computeIfAbsent(generatorName, key -> generateExecutorClass(key, method));

  }

  private Class<MethodExecutor> generateExecutorClass(String generatorName, Method method) {
    DynamicType.Builder<Object> operationWrapperClassBuilder = new ByteBuddy()
        .subclass(Object.class, NO_CONSTRUCTORS)
        .implement(MethodExecutor.class)
        .name(generatorName)
        .defineField(TARGET_INSTANCE_FIELD_NAME, method.getDeclaringClass(), PRIVATE, FINAL);

    for (int i = 0; i < method.getParameterTypes().length; ++i) {
      operationWrapperClassBuilder = operationWrapperClassBuilder
          .defineField(method.getParameters()[i].getName() + "Resolver",
                       parameterizedType(ArgumentResolver.class, method.getParameterTypes()[i]).build(),
                       PRIVATE, FINAL);
    }

    Annotatable<Object> constructorDefinition = operationWrapperClassBuilder
        .defineConstructor(PUBLIC)
        .withParameter(method.getDeclaringClass(), TARGET_INSTANCE_FIELD_NAME);

    for (int i = 0; i < method.getParameterTypes().length; ++i) {
      constructorDefinition = constructorDefinition
          .withParameter(parameterizedType(ArgumentResolver.class, method.getParameterTypes()[i]).build(),
                         method.getParameters()[i].getName() + "Resolver");
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
          stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Object.class)
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
                .filter(named(method.getParameters()[i].getName() + "Resolver"))
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
        .intercept(new Implementation() {

          @Override
          public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
          }

          @Override
          public ByteCodeAppender appender(Target implementationTarget) {
            return (methodVisitor, instrumentationContext, instrumentedMethod) -> {
              List<StackManipulation> stackManipulationItems = new ArrayList<>();

              stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
              stackManipulationItems.add(forField(implementationTarget
                  .getInstrumentedType()
                  .getDeclaredFields()
                  .filter(named(TARGET_INSTANCE_FIELD_NAME)).getOnly())
                      .read());

              for (int i = 0; i < method.getParameterTypes().length; ++i) {
                stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
                stackManipulationItems.add(forField(implementationTarget
                    .getInstrumentedType()
                    .getDeclaredFields()
                    .filter(
                            named(method.getParameters()[i].getName() + "Resolver"))
                    .getOnly())
                        .read());
                stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(1));
                stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(ArgumentResolver.class)
                    .getDeclaredMethods()
                    .filter(named("resolve"))
                    .getOnly()));
                final TypeDescription.ForLoadedType paramType =
                    new TypeDescription.ForLoadedType(method.getParameterTypes()[i]);

                if (paramType.isPrimitive()) {
                  stackManipulationItems.add(TypeCasting.to(paramType.asBoxed()));

                  if (int.class.equals(method.getParameterTypes()[i])) {
                    stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Integer.class)
                        .getDeclaredMethods()
                        .filter(named("intValue"))
                        .getOnly()));
                  } else if (long.class.equals(method.getParameterTypes()[i])) {
                    stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Long.class)
                        .getDeclaredMethods()
                        .filter(named("longValue"))
                        .getOnly()));
                  } else if (short.class.equals(method.getParameterTypes()[i])) {
                    stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Short.class)
                        .getDeclaredMethods()
                        .filter(named("shortValue"))
                        .getOnly()));
                  } else if (double.class.equals(method.getParameterTypes()[i])) {
                    stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Double.class)
                        .getDeclaredMethods()
                        .filter(named("doubleValue"))
                        .getOnly()));
                  } else if (float.class.equals(method.getParameterTypes()[i])) {
                    stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Float.class)
                        .getDeclaredMethods()
                        .filter(named("floatValue"))
                        .getOnly()));
                  } else if (boolean.class.equals(method.getParameterTypes()[i])) {
                    stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Boolean.class)
                        .getDeclaredMethods()
                        .filter(named("booleanValue"))
                        .getOnly()));
                  } else if (char.class.equals(method.getParameterTypes()[i])) {
                    stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Character.class)
                        .getDeclaredMethods()
                        .filter(named("charValue"))
                        .getOnly()));
                  } else if (byte.class.equals(method.getParameterTypes()[i])) {
                    stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Byte.class)
                        .getDeclaredMethods()
                        .filter(named("byteValue"))
                        .getOnly()));
                  }
                } else {
                  stackManipulationItems.add(TypeCasting.to(paramType));
                }
              }

              stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(method.getDeclaringClass())
                  .getDeclaredMethods()
                  .filter(named(method.getName()))
                  .getOnly()));

              if (Void.TYPE.equals(method.getReturnType())) {
                stackManipulationItems.add(NullConstant.INSTANCE);
              } else if (int.class.equals(method.getReturnType())) {
                stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Integer.class)
                    .getDeclaredMethods()
                    .filter(named("valueOf").and(takesArgument(0, int.class)))
                    .getOnly()));
              } else if (long.class.equals(method.getReturnType())) {
                stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Long.class)
                    .getDeclaredMethods()
                    .filter(named("valueOf").and(takesArgument(0, long.class)))
                    .getOnly()));
              } else if (short.class.equals(method.getReturnType())) {
                stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Short.class)
                    .getDeclaredMethods()
                    .filter(
                            named("valueOf").and(takesArgument(0, short.class)))
                    .getOnly()));
              } else if (double.class.equals(method.getReturnType())) {
                stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Double.class)
                    .getDeclaredMethods()
                    .filter(
                            named("valueOf").and(takesArgument(0, double.class)))
                    .getOnly()));
              } else if (float.class.equals(method.getReturnType())) {
                stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Float.class)
                    .getDeclaredMethods()
                    .filter(
                            named("valueOf").and(takesArgument(0, float.class)))
                    .getOnly()));
              } else if (boolean.class.equals(method.getReturnType())) {
                stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Boolean.class)
                    .getDeclaredMethods()
                    .filter(
                            named("valueOf").and(takesArgument(0, boolean.class)))
                    .getOnly()));
              } else if (char.class.equals(method.getReturnType())) {
                stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Character.class)
                    .getDeclaredMethods()
                    .filter(named("valueOf").and(takesArgument(0, char.class)))
                    .getOnly()));
              } else if (byte.class.equals(method.getReturnType())) {
                stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Byte.class)
                    .getDeclaredMethods()
                    .filter(named("valueOf").and(takesArgument(0, byte.class)))
                    .getOnly()));
              }
              stackManipulationItems.add(MethodReturn.of(new TypeDescription.ForLoadedType(Object.class)));

              StackManipulation.Size size = new StackManipulation.Compound(stackManipulationItems)
                  .apply(methodVisitor, instrumentationContext);
              return new ByteCodeAppender.Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
            };
          }
        }).make();

    final File file = new File(TEMP_DIR, generatorName + ".class");
    if (file.exists()) {
      file.delete();
    }

    try (FileOutputStream os = new FileOutputStream(file)) {
      os.write(byteBuddyMadeWrapper.getBytes());
      CompositeClassLoader executorClassLoader = new CompositeClassLoader(method.getDeclaringClass().getClassLoader(),
                                                                          ByteBuddyWrappedMethodComponentExecutor.class
                                                                              .getClassLoader());

      return (Class<MethodExecutor>) byteBuddyMadeWrapper.load(executorClassLoader, INJECTION).getLoaded();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not generate MethodExecutor class"), e);
    }
  }

  private String getGeneratorName(Method method) {
    return method.getDeclaringClass().getName() + "$" + method.getName() + "$" + hashCode() + "$MethodComponentExecutorWrapper";
  }
}
