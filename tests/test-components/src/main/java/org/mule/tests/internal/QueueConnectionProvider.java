package org.mule.tests.internal;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueConnectionProvider implements CachedConnectionProvider<TestQueue> {

  private final Logger LOGGER = LoggerFactory.getLogger(QueueConnectionProvider.class);

  @Inject
  private TestQueueManager queueManager;

  @RefName
  private String configName;

  @Override
  public TestQueue connect() throws ConnectionException {
    return queueManager.get(configName);
  }

  @Override
  public void disconnect(TestQueue connection) {
    queueManager.remove(configName);
  }

  @Override
  public ConnectionValidationResult validate(TestQueue connection) {
    return ConnectionValidationResult.success();
  }
}
