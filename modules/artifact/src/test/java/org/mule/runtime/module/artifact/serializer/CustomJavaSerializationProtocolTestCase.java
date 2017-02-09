/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.serializer;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.artifact.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.serialization.SerializationException;
import org.mule.runtime.core.internal.serialization.AbstractSerializerProtocolContractTestCase;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CustomJavaSerializationProtocolTestCase extends AbstractSerializerProtocolContractTestCase {

  public static final String INSTANCE_NAME = "serializedInstance";
  public static final String SERIALIZABLE_CLASS = "org.foo.SerializableClass";
  public static final String ARTIFACT_ID = "testId";
  public static final String ARTIFACT_NAME = "test";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ClassLoaderRepository classLoaderRepository;

  @Override
  protected void doSetUp() throws Exception {
    classLoaderRepository = mock(ClassLoaderRepository.class);
    when(classLoaderRepository.getId(getClass().getClassLoader())).thenReturn(empty());
    when(classLoaderRepository.getId(null)).thenReturn(empty());
    serializationProtocol = new CustomJavaSerializationProtocol(classLoaderRepository);

    initialiseIfNeeded(serializationProtocol, muleContext);
  }

  @Test(expected = SerializationException.class)
  public void notSerializable() throws Exception {
    serializationProtocol.serialize(new Object());
  }

  @Test
  public final void serializeWithoutDefaultConstructorFromArtifactClassLoader() throws Exception {

    final File compiledClasses = new File(temporaryFolder.getRoot(), "compiledClasses");
    compiledClasses.mkdirs();

    final File sourceFile = new File(getClass().getResource("/org/foo/SerializableClass.java").getFile());

    CompilerUtils.SingleClassCompiler compiler = new CompilerUtils.SingleClassCompiler();
    compiler.compile(sourceFile);

    final URL[] urls = new URL[] {compiler.getTargetFolder().toURL()};
    final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicy.getLookupStrategy(any())).thenReturn(PARENT_FIRST);
    final MuleArtifactClassLoader artifactClassLoader =
        new MuleArtifactClassLoader(ARTIFACT_ID, new ArtifactDescriptor(ARTIFACT_NAME), urls, getClass().getClassLoader(),
                                    lookupPolicy);
    when(classLoaderRepository.getId(artifactClassLoader)).thenReturn(of(ARTIFACT_ID));
    when(classLoaderRepository.find(ARTIFACT_ID)).thenReturn(of(artifactClassLoader));

    final Class<?> echoTestClass = artifactClassLoader.loadClass(SERIALIZABLE_CLASS);
    final Object payload = echoTestClass.newInstance();
    setObjectName(payload);

    Event event = eventBuilder().message(InternalMessage.of(payload)).build();
    byte[] bytes = serializationProtocol.serialize(event.getMessage());

    InternalMessage message = serializationProtocol.deserialize(bytes);
    Object deserialized = message.getPayload().getValue();

    assertThat(deserialized.getClass().getName(), equalTo(SERIALIZABLE_CLASS));
    assertThat(deserialized.getClass().getClassLoader(), equalTo(artifactClassLoader));
    assertThat(deserialized, equalTo(payload));
    assertThat(getObjectName(deserialized), equalTo(INSTANCE_NAME));
  }

  private void setObjectName(Object payload) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final Method setNameMethod = payload.getClass().getMethod("setName", new Class[] {String.class});
    setNameMethod.invoke(payload, INSTANCE_NAME);
  }

  private String getObjectName(Object payload) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final Method getNameMethod = payload.getClass().getMethod("getName");
    return (String) getNameMethod.invoke(payload);
  }

}
