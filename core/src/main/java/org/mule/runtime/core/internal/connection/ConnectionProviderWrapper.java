package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.config.HasPoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.HasReconnectionConfig;

/**
 * Base contract for wrappers of {@link ConnectionProvider} instances
 *
 * @param <C> the generic type of the connections that {@link #getDelegate()} produces
 * @since 4.3.0
 */
public interface ConnectionProviderWrapper<C>
    extends ConnectionProvider<C>, HasPoolingProfile, HasReconnectionConfig, HasDelegate<C>, Lifecycle {

  RetryPolicyTemplate getRetryPolicyTemplate();

}
