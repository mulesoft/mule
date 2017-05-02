/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import org.mule.DefaultMuleContext;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.module.xml.transformer.XsltTransformer;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class XsltTransformerPoolEvictionTestCase
{
    // "excessIdleTransformers" refers to the idle transformers beyond "minIdle"
    @Test
    public void excessIdleTransformersAreEvictedFromPoolWhenTimeBetweenEvictionsIsSpecified()
            throws TransformerException, InterruptedException
    {
        int millisecondsBetweenTransformerEvictions = 2000;
        XsltTransformer transformer =
                createXsltTransformerForEvictionTesting(millisecondsBetweenTransformerEvictions);
        // This test assumes that excess idle transformer eviction works correctly if the following
        // sequence of events can be satisfied:
        // 1) the number of transformers in the pool is increased to a number greater than min idle
        // 2) the number of transformers is reduced, by the pool itself, to min idle
        // This sequence of events can't happen if min idle is below either max active or max idle.
        // Hence, the max idle and max active need to be set to values greater than min idle.
        int minIdleTransformers = transformer.getMinIdleTransformers();
        transformer.setMaxIdleTransformers(minIdleTransformers + 1);
        transformer.setMaxActiveTransformers(minIdleTransformers + 1);

        GenericObjectPool<Transformer> pool = getTransformerPool(transformer);
        // Default min evictable idle time is 30 minutes, so this needs to be reduced for testing
        // purposes. Note that a non-positive value results in no eviction. Hence 1 is chosen since
        // it's the minimum positive long.
        pool.setMinEvictableIdleTimeMillis(1);
        // Transforming minIdle+1 threads at a single time so that the transformer pool grows to
        // to a size greater than minIdle.
        int numberOfSimultaneousTransformers = minIdleTransformers + 1;
        // The numTestsPerEvictionRun needs to be set to the number of transformers so that all idle
        // transformers are evicted from the pool every time eviction runs.
        pool.setNumTestsPerEvictionRun(numberOfSimultaneousTransformers);

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfSimultaneousTransformers);
        DefaultMuleMessage muleMessage = createDummyMuleMessage();
        for (int i = 0; i < numberOfSimultaneousTransformers; i++)
        {
            executorService.execute(
                    new MuleMessageXsltTransformingRunnable(transformer, muleMessage));
        }
        // This thread must wait until all WaitingTransformers.transform() methods have been called.
        Set<WaitingTransformer> waitingTransformersSetAndMutex =
                WaitingTransformerFactory.WAITING_TRANSFORMERS_SET_AND_MUTEX;
        while (waitingTransformersSetAndMutex.size() != numberOfSimultaneousTransformers)
        {
            Thread.sleep(100);
        }

        assertThat(getNumberOfActiveAndIdlePoolObjects(pool), is(greaterThan(minIdleTransformers)));
        synchronized (waitingTransformersSetAndMutex)
        {
            waitingTransformersSetAndMutex.notifyAll();
        }
        // Waiting twice the number of milliseconds between evictions ensures that eviction has
        // run at least once.
        Thread.sleep(2*millisecondsBetweenTransformerEvictions);
        assertThat(getNumberOfActiveAndIdlePoolObjects(pool), is(minIdleTransformers));
    }

    private XsltTransformer createXsltTransformerForEvictionTesting(
            int millisecondsBetweenTransformerEvictions)
    {
        XsltTransformer transformer = new XsltTransformer();
        transformer.setTimeBetweenTransformerEvictions(millisecondsBetweenTransformerEvictions);
        transformer.setXslTransformerFactory(WaitingTransformerFactory.class.getName());
        transformer.setXslt("xsl-text");
        return transformer;
    }

    private int getNumberOfActiveAndIdlePoolObjects(GenericObjectPool<Transformer> pool)
    {
        int numberOfObjects = 0;
        synchronized (pool)
        {
            numberOfObjects += pool.getNumActive();
            numberOfObjects += pool.getNumIdle();
        }
        return numberOfObjects;
    }

    @SuppressWarnings("unchecked")
    private GenericObjectPool<Transformer> getTransformerPool(XsltTransformer transformer)
    {
        Field poolField = ReflectionUtils.findField(XsltTransformer.class, "transformerPool");
        poolField.setAccessible(true);
        return (GenericObjectPool<Transformer>)ReflectionUtils.getField(poolField, transformer);
    }

    private DefaultMuleMessage createDummyMuleMessage()
    {
        DefaultMuleContext muleContext = new DefaultMuleContext();
        muleContext.setMuleConfiguration(new DefaultMuleConfiguration());
        return new DefaultMuleMessage("", muleContext);
    }

    private static class MuleMessageXsltTransformingRunnable implements Runnable
    {
        private final XsltTransformer xsltTransformer;
        private final MuleMessage muleMessage;

        public MuleMessageXsltTransformingRunnable(XsltTransformer transformer,
                MuleMessage muleMessage)
        {
            this.xsltTransformer = transformer;
            this.muleMessage = muleMessage;
        }

        @Override
        public void run()
        {
            try
            {
                xsltTransformer.transformMessage(muleMessage, null);
            }
            catch (org.mule.api.transformer.TransformerException exception)
            {
                throw new RuntimeException(exception);
            }
        }
    }

    // needs to be public for instantiation by XsltTransformer$PooledXsltTransformerFactory
    public static class WaitingTransformerFactory extends TransformerFactory
    {
        /**
         * The list of transformers that will be waiting to be notified during their {@link
         * WaitingTransformer#transform(Source, Result)} method calls.
         *
         * This needs to be static since {@link
         * org.mule.module.xml.transformer.XsltTransformer.PooledXsltTransformerFactory#
         * makeObject()} instantiates a new factory on every call.
         */
        public static final Set<WaitingTransformer> WAITING_TRANSFORMERS_SET_AND_MUTEX =
                Collections.synchronizedSet(new HashSet<WaitingTransformer>());

        @Override
        public Transformer newTransformer(Source source) throws TransformerConfigurationException
        {
            return new WaitingTransformer(WAITING_TRANSFORMERS_SET_AND_MUTEX);
        }

        @Override
        public Transformer newTransformer() throws TransformerConfigurationException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Templates newTemplates(Source source) throws TransformerConfigurationException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Source getAssociatedStylesheet(Source source, String media, String title,
                String charset) throws TransformerConfigurationException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setURIResolver(URIResolver resolver)
        {
            // not throwing UnsupportedOperationException since this method is called during call
            // to XsltTransformer.transformMessage()
        }

        @Override
        public URIResolver getURIResolver()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFeature(String name, boolean value) throws TransformerConfigurationException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getFeature(String name)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAttribute(String name, Object value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(String name)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setErrorListener(ErrorListener listener)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public ErrorListener getErrorListener()
        {
            throw new UnsupportedOperationException();
        }
    }

    private static class WaitingTransformer extends Transformer
    {
        private final Set<WaitingTransformer> waitingTransformersSetAndMutex;

        public WaitingTransformer(Set<WaitingTransformer> waitingTransformersSetAndMutex)
        {
            this.waitingTransformersSetAndMutex = waitingTransformersSetAndMutex;
        }

        /**
         * By waiting here, this test can ensure that there are more than minIdle objects in the
         * XsltTransformer's pool before allowing the transformers to be evicted from the pool.
         */
        @Override
        public void transform(Source xmlSource, Result outputTarget)
                throws javax.xml.transform.TransformerException
        {
            synchronized (waitingTransformersSetAndMutex)
            {
                waitingTransformersSetAndMutex.add(this);
                try
                {
                    waitingTransformersSetAndMutex.wait();
                }
                catch (InterruptedException exception)
                {
                    throw new RuntimeException(exception);
                }
            }
        }

        @Override
        public void setParameter(String name, Object value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getParameter(String name)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearParameters()
        {
            // not throwing UnsupportedOperationException since this method is called during call
            // to XsltTransformer.transformMessage()
        }

        @Override
        public void setURIResolver(URIResolver resolver)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public URIResolver getURIResolver()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOutputProperties(Properties oformat)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Properties getOutputProperties()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOutputProperty(String name, String value) throws IllegalArgumentException
        {
            // not throwing UnsupportedOperationException since this method is called during call
            // to XsltTransformer.transformMessage()
        }

        @Override
        public String getOutputProperty(String name) throws IllegalArgumentException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setErrorListener(ErrorListener listener) throws IllegalArgumentException
        {
            // not throwing UnsupportedOperationException since this method is called during call
            // to XsltTransformer.transformMessage()
        }

        @Override
        public ErrorListener getErrorListener()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reset()
        {
            // not throwing UnsupportedOperationException since this method is called during
            // XsltTransformer.transformMessage(), additionally this method needs to override the
            // super class's implementation because that throws an UnsupportedOperationException.
        }
    }
}
