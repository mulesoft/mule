//COPYRIGHT
package org.mule.ra;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

import javax.resource.spi.security.PasswordCredential;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.ResourceException;
import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.Iterator;

//AUTHOR

public class RaHelper
{
    public static PasswordCredential getPasswordCredential
            (final ManagedConnectionFactory mcf,
             final Subject subject, ConnectionRequestInfo info)
            throws ResourceException {
        if (subject == null) {
            if (info == null) {
                return null;
            } else {

                MuleConnectionRequestInfo muleInfo =
                        (MuleConnectionRequestInfo) info;

                // Can't create a PC with null values
                if (muleInfo.getUsername() == null || muleInfo.getPassword() == null) {
                    //logger.info("\tUtil::GetPasswordCred: User or password is null");
                    return null;
                }

                char[] password = muleInfo.getPassword().toCharArray();

                PasswordCredential pc =
                        new PasswordCredential(muleInfo.getUsername(), password);

                pc.setManagedConnectionFactory(mcf);
                //logger.info("\tUtil::GetPasswordCred: returning a created PC");
                return pc;
            }
        } else {
            PasswordCredential pc =
                    (PasswordCredential) AccessController.doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            Set creds = subject.getPrivateCredentials(PasswordCredential.class);
                            Iterator iter = creds.iterator();
                            while (iter.hasNext()) {
                                PasswordCredential temp =
                                        (PasswordCredential) iter.next();
                                if (temp != null &&
                                        temp.getManagedConnectionFactory() != null &&
                                        temp.getManagedConnectionFactory().equals(mcf)) {
                                    return temp;
                                }
                            }
                            return null;
                        }
                    });
            if (pc == null) {
                throw new java.lang.SecurityException(new Message(Messages.AUTH_NO_CREDENTIALS).getMessage());
            } else {
                return pc;
            }
        }
    }

    /**
     * Determines whether two PasswordCredentials are the same.
     *
     * @param a  first PasswordCredential
     * @param b  second PasswordCredential
     *
     * @return  true if the two parameters are equal; false otherwise
     */

    static public boolean isPasswordCredentialEqual(PasswordCredential a, PasswordCredential b)
    {
        if (a == b)
	    return true;
        if ((a == null) && (b != null))
	    return false;
        if ((a != null) && (b == null))
	    return false;

        if (!isEqual(a.getUserName(), b.getUserName()))
	    return false;

        String p1 = null;
        String p2 = null;

        if (a.getPassword() != null)
	{
            p1 = new String(a.getPassword());
        }
        if (b.getPassword() != null)
	{
            p2 = new String(b.getPassword());
        }
        return (isEqual(p1, p2));
    }

    static public boolean isEqual(String a, String b)
    {
        if (a == null)
	{
            return (b == null);
        } else {
            return a.equals(b);
        }
    }
}
