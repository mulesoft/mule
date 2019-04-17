/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.graph;

import static java.util.Collections.synchronizedList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.CompositeConverter;
import org.mule.transformer.builder.MockConverterBuilder;
import org.mule.transformer.builder.MockTransformerBuilder;

@SmallTest
public class GraphTransformerResolverTestCase extends AbstractMuleTestCase
{
    private static final int CONCURRENCY_TEST_SIZE = 400;
    private static final int MAX_TIMEOUT_SECONDS = 20;
    private static final DataType XML_DATA_TYPE = mock(DataType.class, "XML_DATA_TYPE");
    private static final DataType JSON_DATA_TYPE = mock(DataType.class, "JSON_DATA_TYPE");
    private static final DataType INPUT_STREAM_DATA_TYPE = mock(DataType.class, "INPUT_STREAM_DATA_TYPE");
    private static final DataType STRING_DATA_TYPE = mock(DataType.class, "STRING_DATA_TYPE");

    private static class XML_CLASS
    {

    }

    private static class JSON_CLASS
    {

    }

    private static class INPUT_STREAM_CLASS
    {

    }

    private static class STRING_CLASS
    {

    }

    @BeforeClass
    public static void setupDataTypes()
    {
        doReturn(true).when(XML_DATA_TYPE).isCompatibleWith(XML_DATA_TYPE);
        doReturn(XML_CLASS.class).when(XML_DATA_TYPE).getType();
        doReturn(true).when(JSON_DATA_TYPE).isCompatibleWith(JSON_DATA_TYPE);
        doReturn(JSON_CLASS.class).when(JSON_DATA_TYPE).getType();
        doReturn(true).when(INPUT_STREAM_DATA_TYPE).isCompatibleWith(INPUT_STREAM_DATA_TYPE);
        doReturn(INPUT_STREAM_CLASS.class).when(INPUT_STREAM_DATA_TYPE).getType();
        doReturn(true).when(STRING_DATA_TYPE).isCompatibleWith(STRING_DATA_TYPE);
        doReturn(STRING_CLASS.class).when(STRING_DATA_TYPE).getType();
    }

    private GraphTransformerResolver graphResolver = new GraphTransformerResolver();

    @Test
    public void cachesResolvedTransformer() throws ResolverException
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertThat(transformer1, sameInstance(transformer2));
    }

    @Test
    public void clearsCacheWhenAddsConverter() throws ResolverException
    {
        Converter xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter inputStreamToXml = new MockConverterBuilder().named("inputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertThat(transformer1, notNullValue());

        Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
        graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
    }

    @Test
    public void ignoresAddedTransformer() throws ResolverException
    {
        Converter xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter inputStreamToXml = new MockConverterBuilder().named("inputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertThat(transformer1, notNullValue());

        Transformer xmlToString = new MockTransformerBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
        graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertThat(transformer1, sameInstance(transformer2));
    }

    @Test
    public void ignoresRemovedTransformer() throws ResolverException
    {
        Converter xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        Converter inputStreamToXml = new MockConverterBuilder().named("inputStreamToXml").from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer1 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertThat(transformer1, notNullValue());

        Transformer xmlToString = new MockTransformerBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).build();
        graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.REMOVED);

        Transformer transformer2 = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertThat(transformer1, sameInstance(transformer2));
    }

    @Test
    public void clearsCacheWhenRemovesTransformer() throws ResolverException
    {
        Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        when(xmlToJson.getName()).thenReturn("xmlToJson");
        Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();
        when(inputStreamToXml.getName()).thenReturn("inputStreamToXml");

        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
        assertThat(transformer, notNullValue());

        graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.REMOVED);

        transformer = graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);

        assertThat(transformer, nullValue());
    }

    @Test
    public void resolvesTransformersWithDifferentLength() throws ResolverException
    {
        Converter xmlToInputStream = new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();
        Converter xmlToJson = new MockConverterBuilder().named("xmlToJson").from(XML_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();
        Converter inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();

        graphResolver.transformerChange(xmlToInputStream, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(inputStreamToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertThat(xmlToJson, sameInstance(transformer));
    }

    @Test
    public void resolvesTransformersWithSameLengthAndDifferentWeight() throws ResolverException
    {
        Converter xmlToInputStream = new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();
        Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();
        Converter inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).weighting(2).build();
        Converter stringToJson = new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();

        graphResolver.transformerChange(xmlToInputStream, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(inputStreamToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(stringToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertThat(transformer, instanceOf(CompositeConverter.class));
        CompositeConverter compositeConverter = (CompositeConverter) transformer;
        assertThat(compositeConverter.getConverters(), hasSize(2));
        assertThat(xmlToInputStream, sameInstance(compositeConverter.getConverters().get(0)));
        assertThat(inputStreamToJson, sameInstance(compositeConverter.getConverters().get(1)));
    }

    @Test
    public void resolvesTransformerWithSameLengthAndSameWeight() throws ResolverException
    {
        Converter xmlToInputStream = new MockConverterBuilder().named("xmlToInputStream").from(XML_DATA_TYPE).to(INPUT_STREAM_DATA_TYPE).weighting(1).build();
        Converter xmlToString = new MockConverterBuilder().named("xmlToString").from(XML_DATA_TYPE).to(STRING_DATA_TYPE).weighting(1).build();
        Converter inputStreamToJson = new MockConverterBuilder().named("inputStreamToJson").from(INPUT_STREAM_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();
        Converter stringToJson = new MockConverterBuilder().named("stringToJson").from(STRING_DATA_TYPE).to(JSON_DATA_TYPE).weighting(1).build();

        graphResolver.transformerChange(xmlToInputStream, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(xmlToString, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(inputStreamToJson, TransformerResolver.RegistryAction.ADDED);
        graphResolver.transformerChange(stringToJson, TransformerResolver.RegistryAction.ADDED);

        Transformer transformer = graphResolver.resolve(XML_DATA_TYPE, JSON_DATA_TYPE);

        assertThat(transformer, instanceOf(CompositeConverter.class));
        CompositeConverter compositeConverter = (CompositeConverter) transformer;
        assertThat(compositeConverter.getConverters(), hasSize(2));
        assertThat(xmlToInputStream, sameInstance(compositeConverter.getConverters().get(0)));
        assertThat(inputStreamToJson, sameInstance(compositeConverter.getConverters().get(1)));
    }

    @Test
    public void modifyGraphWhileResolvingTransformer() throws ResolverException, InterruptedException
    {
        final Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
        final Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

        Runnable addTransformer = new Runnable()
        {
            @Override
            public void run()
            {
                graphResolver.transformerChange(xmlToJson, TransformerResolver.RegistryAction.ADDED);
                graphResolver.transformerChange(inputStreamToXml, TransformerResolver.RegistryAction.ADDED);
            }
        };
        Runnable resolveTransformer = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    graphResolver.resolve(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
                }
                catch (ResolverException e)
                {
                    throw new RuntimeException("Error while getting transformer", e);
                }
            }
        };

        List<Runnable> runnables = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY_TEST_SIZE; i++)
        {
            runnables.add(addTransformer);
            runnables.add(resolveTransformer);
        }

        assertConcurrent("Modify transformers while resolving it", runnables, MAX_TIMEOUT_SECONDS);
    }

    public static void assertConcurrent(final String message, final List<? extends Runnable> runnables, final int maxTimeoutSeconds) throws InterruptedException
    {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = synchronizedList(new ArrayList<Throwable>());
        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try
        {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
            final CountDownLatch afterInitBlocker = new CountDownLatch(1);
            final CountDownLatch allDone = new CountDownLatch(numThreads);
            for (final Runnable submittedTestRunnable : runnables)
            {
                threadPool.submit(new Runnable()
                {
                    public void run()
                    {
                        allExecutorThreadsReady.countDown();
                        try
                        {
                            afterInitBlocker.await();
                            submittedTestRunnable.run();
                        }
                        catch (final Throwable e)
                        {
                            exceptions.add(e);
                        }
                        finally
                        {
                            allDone.countDown();
                        }
                    }
                });
            }
            // wait until all threads are ready
            assertThat("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent",
                    allExecutorThreadsReady.await(runnables.size() * 10, MILLISECONDS));
            // start all test runners
            afterInitBlocker.countDown();
            assertThat(message + " timeout! More than" + maxTimeoutSeconds + "seconds", allDone.await(maxTimeoutSeconds, SECONDS));
        }
        finally
        {
            threadPool.shutdownNow();
        }
        assertThat(exceptions, empty());
    }
}
