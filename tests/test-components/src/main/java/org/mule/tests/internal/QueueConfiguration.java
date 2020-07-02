package org.mule.tests.internal;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

@Configuration(name = "queueConfig")
@Operations(TestComponentsOperations.class)
@ConnectionProviders(QueueConnectionProvider.class)
public class QueueConfiguration {

}
