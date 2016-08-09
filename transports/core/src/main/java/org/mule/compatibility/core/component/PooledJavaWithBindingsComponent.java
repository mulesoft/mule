/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.component;

import org.mule.compatibility.core.api.component.InterfaceBinding;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.component.LifecycleAdapter;
import org.mule.runtime.core.api.lifecycle.InitialisationCallback;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.api.object.ObjectFactory;
import org.mule.runtime.core.config.PoolingProfile;
import org.mule.runtime.core.util.pool.DefaultLifecycleEnabledObjectPool;
import org.mule.runtime.core.util.pool.LifecyleEnabledObjectPool;
import org.mule.runtime.core.util.pool.ObjectPool;

import java.util.List;

/**
 * <code>PooledJavaComponent</code> implements pooling.
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class PooledJavaWithBindingsComponent extends AbstractJavaWithBindingsComponent {

  protected PoolingProfile poolingProfile;
  protected LifecyleEnabledObjectPool lifecycleAdapterPool;

  public PooledJavaWithBindingsComponent() {
    super();
  }

  public PooledJavaWithBindingsComponent(ObjectFactory objectFactory) {
    this(objectFactory, null);
  }

  public PooledJavaWithBindingsComponent(ObjectFactory objectFactory, PoolingProfile poolingProfile) {
    super(objectFactory);
    this.poolingProfile = poolingProfile;
  }

  public PooledJavaWithBindingsComponent(ObjectFactory objectFactory, PoolingProfile poolingProfile,
                                         EntryPointResolverSet entryPointResolverSet, List<InterfaceBinding> bindings) {
    super(objectFactory, entryPointResolverSet, bindings);
    this.poolingProfile = poolingProfile;
  }

  @Override
  protected LifecycleAdapter borrowComponentLifecycleAdaptor() throws Exception {
    return (LifecycleAdapter) lifecycleAdapterPool.borrowObject();
  }

  @Override
  protected void returnComponentLifecycleAdaptor(LifecycleAdapter lifecycleAdapter) {
    lifecycleAdapterPool.returnObject(lifecycleAdapter);
  }

  @Override
  protected void doStart() throws MuleException {
    super.doStart();
    // Wrap pool's objectFactory with a LifeCycleAdaptor factory so we pool
    // LifeCycleAdaptor's and not just pojo instances.
    lifecycleAdapterPool = new DefaultLifecycleEnabledObjectPool(new LifeCycleAdapterFactory(), poolingProfile, muleContext);
    lifecycleAdapterPool.initialise();
    lifecycleAdapterPool.start();
  }

  @Override
  protected void doStop() throws MuleException {
    super.doStop();
    if (lifecycleAdapterPool != null) {
      lifecycleAdapterPool.stop();
      lifecycleAdapterPool.close();
      lifecycleAdapterPool = null;
    }
  }

  public void setPoolingProfile(PoolingProfile poolingProfile) {
    // TODO What happens if this is set while component is started? Should we i)
    // do nothing ii) issue warning iii) stop/start the pool
    // (!!) iv) throw exception?
    this.poolingProfile = poolingProfile;
  }

  public PoolingProfile getPoolingProfile() {
    return poolingProfile;
  }

  /**
   * <code>LifeCycleAdaptorFactory</code> wraps the Component' s {@link ObjectFactory}. The LifeCycleAdaptorFactory
   * <code>getInstance()</code> method creates a new {@link LifecycleAdapter} wrapping the object instance obtained for the
   * component instance {@link ObjectFactory} set on the {@link Component}. <br/>
   * This allows us to keep {@link LifecycleAdapter} creation in the Component and out of the
   * {@link DefaultLifecycleEnabledObjectPool} and to use the generic {@link ObjectPool} interface.
   */
  protected class LifeCycleAdapterFactory implements ObjectFactory {

    @Override
    public Object getInstance(MuleContext context) throws Exception {
      return createLifecycleAdaptor();
    }

    @Override
    public Class<?> getObjectClass() {
      return LifecycleAdapter.class;
    }

    @Override
    public void initialise() throws InitialisationException {
      objectFactory.initialise();
    }

    @Override
    public void dispose() {
      objectFactory.dispose();
    }

    @Override
    public void addObjectInitialisationCallback(InitialisationCallback callback) {
      objectFactory.addObjectInitialisationCallback(callback);
    }

    @Override
    public boolean isSingleton() {
      return false;
    }

    @Override
    public boolean isExternallyManagedLifecycle() {
      return objectFactory.isExternallyManagedLifecycle();
    }

    @Override
    public boolean isAutoWireObject() {
      return objectFactory.isAutoWireObject();
    }
  }
}
