//COPYRIGHT
package org.mule.ra;

import javax.naming.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import java.io.Serializable;

//AUTHOR

public interface MuleConnectionFactory extends Referenceable, Serializable {
    MuleConnection createConnection() throws ResourceException;

    MuleConnection createConnection(MuleConnectionRequestInfo info) throws ResourceException;

    ConnectionManager getManager();

    void setManager(ConnectionManager manager);

    MuleManagedConnectionFactory getFactory();

    void setFactory(MuleManagedConnectionFactory factory);

    MuleConnectionRequestInfo getInfo();

    void setInfo(MuleConnectionRequestInfo info);
}
