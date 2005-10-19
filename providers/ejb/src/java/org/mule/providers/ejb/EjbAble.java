package org.mule.providers.ejb;

/**
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */

public interface EjbAble
{
    /**
     * @return EJB remote method argument classtypes for call
     */
    public Class[] argumentClasses();

    /**
     * @return EJB remote method arguments that are used on EJB call
     */
    public Object[] arguments();
}
