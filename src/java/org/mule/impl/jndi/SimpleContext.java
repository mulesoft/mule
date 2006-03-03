/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.impl.jndi;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
  public class SimpleContext implements Context
{
    /** What holds the bindings. */
 	Map bindings = new HashMap();

 	/** Context's environment. */
 	private Hashtable environment;
     
  	public SimpleContext() {
  	}

    public Object lookupLink(Name name) throws NamingException {
        return null;
    }


    public void rename(Name oldName, Name newName) throws NamingException {

    }


    public NameParser getNameParser(Name name) throws NamingException {
        return null;
    }

    public NamingEnumeration list(Name name) throws NamingException {
        return null;
    }

    public Object lookup(Name name) throws NamingException {
  		return lookup(name.toString());
  	}
  	public Object lookup(String name) throws NamingException {
  		Object rc = bindings.get(name);
  		if (rc == null) {
            throw new NameNotFoundException(name);
        }
  		return rc;
  	}
  	public void bind(Name name, Object obj) throws NamingException {
  		bind(name.toString(), obj);
  	}
  	public void bind(String name, Object obj) throws NamingException {
  		if (bindings.containsKey(name)) {
            throw new NameAlreadyBoundException(name);
        }
  		bindings.put(name, obj);
 	}
  	public void rebind(Name name, Object obj) throws NamingException {
  		rebind(name.toString(), obj);
  	}
  	public void rebind(String name, Object obj) throws NamingException {
  		bindings.put(name, obj);
  	}
  	public void unbind(Name name) throws NamingException {
  		unbind(name.toString());
  	}
  	public void unbind(String name) throws NamingException {
  		if (bindings.remove(name) == null) {
            throw new NameNotFoundException(name);
        }
  	}
  	public void rename(Attributes.Name oldName, Attributes.Name newName) throws NamingException {
  		rename(oldName.toString(), newName.toString());
  	}
  	public void rename(String oldName, String newName) throws NamingException {
  		if (!bindings.containsKey(oldName)) {
            throw new NameNotFoundException(oldName);
        }
  		if (bindings.containsKey(newName)) {
            throw new NameAlreadyBoundException(newName);
        }

  		bindings.put(newName, bindings.remove(oldName));
  	}
  	public NamingEnumeration list(Attributes.Name name) throws NamingException {
  		return list(name.toString());
  	}
  	public NamingEnumeration list(String name) throws NamingException {
  		if (name.length() > 0) {
            throw new OperationNotSupportedException("subcontexts not supported");
        }
  		final Iterator i = bindings.entrySet().iterator();
  		return new NamingEnumeration() {
  			public Object next() {
  				Map.Entry e = (Map.Entry) i.next();
  				return new NameClassPair((String) e.getKey(), e.getValue().getClass().getName());
  			}
  			public boolean hasMore() {
 				return i.hasNext();
 			}
 			public void close() {}
 			public boolean hasMoreElements() {
 				return hasMore();
 			}
 			public Object nextElement() {
 				return next();
 			}
 		};
 	}
 	public NamingEnumeration listBindings(Name name) throws NamingException {
 		return listBindings(name.toString());
 	}
 	public NamingEnumeration listBindings(String name) throws NamingException {
 		if (name.length() > 0) {
            throw new OperationNotSupportedException("subcontexts not supported");
        }
 		final Iterator i = bindings.entrySet().iterator();
 		return new NamingEnumeration() {
 			public Object next() {
 				Map.Entry e = (Map.Entry) i.next();
 				return new Binding((String) e.getKey(), e.getValue());
 			}
 			public boolean hasMore() {
 				return i.hasNext();
 			}
 			public void close() {}
 			public boolean hasMoreElements() {
 				return hasMore();
 			}
 			public Object nextElement() {
 				return next();
 			}

 		};
 	}
 	public void destroySubcontext(Name name) throws NamingException {
 		destroySubcontext(name.toString());
 	}
 	public void destroySubcontext(String name) throws NamingException {
 		throw new OperationNotSupportedException("subcontexts not supported");
 	}
 	public Context createSubcontext(Name name) throws NamingException {
 		return createSubcontext(name.toString());
 	}
 	public Context createSubcontext(String name) throws NamingException {
 		throw new OperationNotSupportedException("subcontexts not supported");
 	}
 	public Object lookupLink(Attributes.Name name) throws NamingException {
 		return lookupLink(name.toString());
 	}
 	public Object lookupLink(String name) throws NamingException {
 		return lookup(name);
 	}
 	public NameParser getNameParser(Attributes.Name name) {
 		return getNameParser(name.toString());
 	}
 	public NameParser getNameParser(String name) {
 		throw new UnsupportedOperationException("getNameParser");
 	}
 	public String composeName(String name, String prefix) throws NamingException {
 		return composeName(new CompositeName(name), new CompositeName(prefix)).toString();
 	}
 	public Name composeName(Name name, Name prefix) throws NamingException {
 		Name result = (Name) prefix.clone();
 		result.addAll(name);
 		return result;
 	}
 	public Object addToEnvironment(String key, Object val) {
 		if (environment == null) {
            environment = new Hashtable();
        }
 		return environment.put(key, val);
 	}
 	public Object removeFromEnvironment(String key) {
 		if (environment == null) {
            environment = new Hashtable();
        }
 		return environment.remove(key);
 	}
 	public Hashtable getEnvironment() {
 		if (environment == null) {
            environment = new Hashtable();
        }
 		return environment;
 	}
 	public void close() {}
 	public String getNameInNamespace() {
 		return "";
 	}

 }
