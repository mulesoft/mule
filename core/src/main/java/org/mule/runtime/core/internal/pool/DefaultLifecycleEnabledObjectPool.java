/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.pool;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.component.LifecycleAdapter;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.object.ObjectFactory;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.runtime.core.internal.util.pool.CommonsPoolObjectPool;
import org.mule.runtime.core.api.util.pool.LifecyleEnabledObjectPool;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool.PoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A LifecyleEnabledObjectPool implementation for pooling {@link LifecycleAdapter} instances for implementations of
 * {@link JavaComponent} that require {@link LifecycleAdapter} pooling such as {@link PooledJavaComponent}.
 * 
 * @see PooledJavaComponent
 */
public class DefaultLifecycleEnabledObjectPool implements LifecyleEnabledObjectPool {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(DefaultLifecycleEnabledObjectPool.class);
  private final MuleContext muleContext;

  protected AtomicBoolean started = new AtomicBoolean(false);

  private List items = new LinkedList();
  private final CommonsPoolObjectPool pool;

  /**
   * @param objectFactory The object factory that should be used to create new
   *        {@link org.mule.runtime.core.api.component.LifecycleAdapter} instance for the pool
   * @param poolingProfile The pooling progile ot be used to configure pool
   * @param muleContext
   */
  public DefaultLifecycleEnabledObjectPool(ObjectFactory objectFactory, PoolingProfile poolingProfile, MuleContext muleContext) {
    this.muleContext = muleContext;
    pool = new CommonsPoolObjectPool(objectFactory, poolingProfile, muleContext) {

      protected PoolableObjectFactory getPooledObjectFactory() {
        return new LifecycleEnabledPoolabeObjectFactoryAdapter();
      }
    };
  }

  public void start() throws MuleException {
    started.set(true);
    synchronized (items) {
      for (Iterator i = items.iterator(); i.hasNext();) {
        ((Startable) i.next()).start();
      }
    }
  }

  public void stop() throws MuleException {
    started.set(false);
    synchronized (items) {
      for (Iterator i = items.iterator(); i.hasNext();) {
        ((Stoppable) i.next()).stop();
      }
    }
  }

  @Override
  public Object borrowObject() throws Exception {
    return pool.borrowObject();
  }

  @Override
  public void returnObject(Object object) {
    pool.returnObject(object);
  }

  @Override
  public int getNumActive() {
    return pool.getNumActive();
  }

  @Override
  public int getMaxActive() {
    return pool.getMaxActive();
  }

  @Override
  public void clear() {
    pool.clear();
  }

  @Override
  public void close() {
    pool.close();
  }

  @Override
  public void setObjectFactory(ObjectFactory objectFactory) {
    pool.setObjectFactory(objectFactory);
  }

  @Override
  public ObjectFactory getObjectFactory() {
    return pool.getObjectFactory();
  }

  @Override
  public void dispose() {
    pool.dispose();
  }

  @Override
  public void initialise() throws InitialisationException {
    pool.initialise();
  }

  /**
   * Wraps org.mule.runtime.core.object.ObjectFactory with commons-pool PoolableObjectFactory
   */
  class LifecycleEnabledPoolabeObjectFactoryAdapter implements PoolableObjectFactory {

    public void activateObject(Object obj) throws Exception {
      // nothing to do
    }

    public void destroyObject(Object obj) throws Exception {
      // Only stop existing objects if they havn't already been stopped
      if (started.get() && obj instanceof Stoppable) {
        ((Stoppable) obj).stop();
      }
      if (obj instanceof Disposable) {
        ((Disposable) obj).dispose();
      }
      synchronized (items) {
        items.remove(obj);
      }
    }

    public Object makeObject() throws Exception {
      Object object = getObjectFactory().getInstance(muleContext);
      // Only start newly created objects if pool is started
      if (started.get() && object instanceof Startable) {
        ((Startable) object).start();
      }
      synchronized (items) {
        items.add(object);
      }
      return object;
    }

    public void passivateObject(Object obj) throws Exception {
      // nothing to do
    }

    public boolean validateObject(Object obj) {
      return true;
    }
  }
}
