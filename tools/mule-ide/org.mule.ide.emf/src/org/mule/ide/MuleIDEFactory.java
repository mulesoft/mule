/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.ide;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.mule.ide.MuleIDEPackage
 * @generated
 */
public interface MuleIDEFactory extends EFactory {
    /**
     * The singleton instance of the factory.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    MuleIDEFactory eINSTANCE = new org.mule.ide.impl.MuleIDEFactoryImpl();

    /**
     * Returns a new object of class '<em>Config File Ref Type</em>'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return a new object of class '<em>Config File Ref Type</em>'.
     * @generated
     */
    ConfigFileRefType createConfigFileRefType();

    /**
     * Returns a new object of class '<em>Config File Type</em>'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return a new object of class '<em>Config File Type</em>'.
     * @generated
     */
    ConfigFileType createConfigFileType();

    /**
     * Returns a new object of class '<em>Config Set Type</em>'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return a new object of class '<em>Config Set Type</em>'.
     * @generated
     */
    ConfigSetType createConfigSetType();

    /**
     * Returns a new object of class '<em>Document Root</em>'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return a new object of class '<em>Document Root</em>'.
     * @generated
     */
    DocumentRoot createDocumentRoot();

    /**
     * Returns a new object of class '<em>Mule Ide Config Type</em>'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return a new object of class '<em>Mule Ide Config Type</em>'.
     * @generated
     */
    MuleIdeConfigType createMuleIdeConfigType();

    /**
     * Returns the package supported by this factory.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return the package supported by this factory.
     * @generated
     */
    MuleIDEPackage getMuleIDEPackage();

} //MuleIDEFactory
