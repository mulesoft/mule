package org.mule.api.security.tls;

import java.security.KeyStore;

import javax.net.ssl.ManagerFactoryParameters;

public interface RevocationCheck
{
    ManagerFactoryParameters configFor(KeyStore trustStore);
}
