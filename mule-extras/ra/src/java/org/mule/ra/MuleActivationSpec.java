//COPYRIGHT
package org.mule.ra;

import org.mule.util.Utility;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.impl.endpoint.MuleEndpointURI;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.ResourceException;
import java.util.Properties;

//AUTHOR

public class MuleActivationSpec implements ActivationSpec
{
    private Properties propertiesMap;
    private String endpointName;
    private String connectorName;
    private int createConnector;
    private MuleResourceAdapter resourceAdapter;
    private UMOEndpointURI endpoint;

    public Properties getPropertiesMap() {
        return propertiesMap;
    }

    public void setPropertiesMap(Properties propertiesMap) {
        this.propertiesMap = propertiesMap;
    }

    public void setPropertiesMap(String properties) {
        String[] pairs = Utility.split(properties,  ",");
        propertiesMap = new Properties();
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            int x = pair.indexOf("=");
            if(x==-1) {
                propertiesMap.setProperty(pair, null);
            } else {
                propertiesMap.setProperty(pair.substring(0, x), pair.substring(x+1));
            }
        }
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public int getCreateConnector() {
        return createConnector;
    }

    public void setCreateConnector(int createConnector) {
        this.createConnector = createConnector;
    }


    public UMOEndpointURI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(UMOEndpointURI endpoint) {
        this.endpoint = endpoint;
    }

    public void setEndpoint(String endpoint) throws MalformedEndpointException {
        this.endpoint = new MuleEndpointURI(endpoint);
        if(propertiesMap!=null) {
            propertiesMap.putAll(this.endpoint.getParams());
        } else {
            propertiesMap = this.endpoint.getParams();
        }
    }

    public void validate() throws InvalidPropertyException
    {
        if(endpoint==null) {
            throw new InvalidPropertyException("endpoint");
        }
    }

    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceException {
        //spec section 5.3.3
        if (this.resourceAdapter != null) {
            throw new ResourceException("ResourceAdapter already set");
        }
        if (!(resourceAdapter instanceof MuleResourceAdapter)) {
            throw new ResourceException("ResourceAdapter is not of type: " + MuleResourceAdapter.class.getName());
        }
        this.resourceAdapter = (MuleResourceAdapter) resourceAdapter;
    }
}
