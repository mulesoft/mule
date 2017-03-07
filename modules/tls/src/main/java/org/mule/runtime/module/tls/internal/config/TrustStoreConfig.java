package org.mule.runtime.module.tls.internal.config;

import org.mule.runtime.api.tls.TlsContextTrustStoreConfiguration;

/**
 * POJO for parsing the TLS trust store before building the actual DefaultTlsContext
 *
 * @since 4.0
 */
public class TrustStoreConfig implements TlsContextTrustStoreConfiguration {
    private String path;
    private String password;
    private String type;
    private String algorithm;
    private boolean insecure;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public boolean isInsecure() {
        return insecure;
    }

    public void setInsecure(boolean insecure) {
        this.insecure = insecure;
    }
}
