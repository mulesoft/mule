//COPYRIGHT
package org.mule.extras.spring.events;

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.mule.TestConnector;

//AUTHOR

public class TestConnectorWithContainerDependents extends TestConnector
{
    private Apple containerProp;

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#doInitialise()
     */
    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        if(containerProp==null) {
            throw new IllegalStateException("Initialise should not be called before all properties have been set");
        }

    }

    public Apple getContainerProp()
    {
        return containerProp;
    }

    public void setContainerProp(Apple containerProp)
    {
        this.containerProp = containerProp;
    }
}
