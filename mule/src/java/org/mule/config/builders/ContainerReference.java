//COPYRIGHT
package org.mule.config.builders;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.model.ComponentNotFoundException;
import org.mule.umo.model.ComponentResolverException;
import org.mule.umo.model.UMOContainerContext;

import java.util.List;
import java.util.Map;

/**
 * <code>ContainerReference</code> maintains a container reference for the
 * MuleXmlConfigurationBuilder that gets wired once the configuration documents
 * have been loaded
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ContainerReference
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(ContainerReference.class);

    private String propertyName;
    private String containerRef;
    private Object object;
    private boolean required;

    public ContainerReference(String propertyName, String containerRef, Object object, boolean required)
    {
        this.propertyName = propertyName;
        this.containerRef = containerRef;
        this.object = object;
        this.required = required;
    }

    public void resolveReference(UMOContainerContext ctx) throws ComponentResolverException
    {
        Object comp = null;
        try
        {
            comp = ctx.getComponent(containerRef);
        } catch (ComponentNotFoundException e)
        {
            if (required)
            {
                throw e;
            } else
            {
                logger.warn("Component reference not found: " + e.getMessage());
                return;
            }
        }
        try
        {
            if (object instanceof Map)
            {
                ((Map) object).put(propertyName, comp);
            } else if (object instanceof List)
            {
                ((List) object).add(comp);
            } else
            {
                BeanUtils.setProperty(object, propertyName, comp);
            }
        } catch (Exception e)
        {
            throw new ComponentResolverException("Failed to set property: " + propertyName + " on object: " +
                    object.getClass().getName() + " with parameter type: " + comp.getClass().getName());
        }
    }
}
