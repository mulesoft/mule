/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.config.bootstrap.TestBootstrapServiceDiscoverer;
import org.mule.runtime.core.internal.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.transformer.AbstractDiscoverableTransformer;
import org.mule.runtime.core.internal.transformer.ResolverException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SimpleRegistryBootstrapTransformersTestCase extends AbstractMuleContextTestCase {

  @Test
  public void registeringTransformersWithCustomKey() throws MuleException {
    Properties properties = new Properties();
    properties.setProperty("core.transformer.1", ExpectedKeyTransformer.class.getName());
    properties.setProperty("custom1", CustomKeyTransformer.class.getName());

    TestTransformerResolver transformerResolver = new TestTransformerResolver();
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject("testTransformerResolver", transformerResolver);

    final BootstrapServiceDiscoverer bootstrapServiceDiscoverer = new TestBootstrapServiceDiscoverer(properties);
    muleContext.setBootstrapServiceDiscoverer(bootstrapServiceDiscoverer);

    SimpleRegistryBootstrap registryBootstrap = new SimpleRegistryBootstrap(APP, muleContext);
    registryBootstrap.initialise();

    assertEquals(2, transformerResolver.getTransformersCount());

    assertTrue(transformerResolver.contains(ExpectedKeyTransformer.class));

    assertTrue(transformerResolver.contains(CustomKeyTransformer.class));
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    // Avoid to use DefaultsConfigurationBuilder because it registers a new instance of SimpleRegistryBootstrap
    // that conflicts with the one being tested in the test
    return new DummyConfigurationBuilder();
  }

  private class DummyConfigurationBuilder extends DefaultsConfigurationBuilder {

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception {
      // Do nothing
    }
  }

  public static class ExpectedKeyTransformer extends AbstractDiscoverableTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      return null;
    }
  }

  public static class CustomKeyTransformer extends AbstractDiscoverableTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      return null;
    }
  }

  private static class TestTransformerResolver implements TransformerResolver {

    private List<Transformer> transformers = new ArrayList<>();

    @Override
    public Transformer resolve(DataType source, DataType result) throws ResolverException {
      return null;
    }

    @Override
    public void transformerChange(Transformer transformer, RegistryAction registryAction) {
      transformers.add(transformer);
    }

    public int getTransformersCount() {
      return transformers.size();
    }

    private boolean contains(Class<? extends Transformer> transformerClass) {
      for (Transformer transformer : transformers) {
        if (transformerClass.isAssignableFrom(transformer.getClass())) {
          return true;
        }
      }
      return false;
    }
  }
}
