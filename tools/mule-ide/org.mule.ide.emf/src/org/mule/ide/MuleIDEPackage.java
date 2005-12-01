/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.ide;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see org.mule.ide.MuleIDEFactory
 * @model kind="package"
 * @generated
 */
public interface MuleIDEPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "ide";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "platform:/resource/org.mule.ide.emf/xml/mule-ide.xsd";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "MuleIde";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	MuleIDEPackage eINSTANCE = org.mule.ide.impl.MuleIDEPackageImpl.init();

	/**
	 * The meta object id for the '{@link org.mule.ide.impl.ConfigFileRefTypeImpl <em>Config File Ref Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.ide.impl.ConfigFileRefTypeImpl
	 * @see org.mule.ide.impl.MuleIDEPackageImpl#getConfigFileRefType()
	 * @generated
	 */
	int CONFIG_FILE_REF_TYPE = 0;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_FILE_REF_TYPE__ID = 0;

	/**
	 * The number of structural features of the the '<em>Config File Ref Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_FILE_REF_TYPE_FEATURE_COUNT = 1;

	/**
	 * The meta object id for the '{@link org.mule.ide.impl.ConfigFileTypeImpl <em>Config File Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.ide.impl.ConfigFileTypeImpl
	 * @see org.mule.ide.impl.MuleIDEPackageImpl#getConfigFileType()
	 * @generated
	 */
	int CONFIG_FILE_TYPE = 1;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_FILE_TYPE__DESCRIPTION = 0;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_FILE_TYPE__ID = 1;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_FILE_TYPE__PATH = 2;

	/**
	 * The number of structural features of the the '<em>Config File Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_FILE_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.ide.impl.ConfigSetTypeImpl <em>Config Set Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.ide.impl.ConfigSetTypeImpl
	 * @see org.mule.ide.impl.MuleIDEPackageImpl#getConfigSetType()
	 * @generated
	 */
	int CONFIG_SET_TYPE = 2;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_SET_TYPE__DESCRIPTION = 0;

	/**
	 * The feature id for the '<em><b>Config File Ref</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_SET_TYPE__CONFIG_FILE_REF = 1;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_SET_TYPE__ID = 2;

	/**
	 * The number of structural features of the the '<em>Config Set Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONFIG_SET_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.ide.impl.DocumentRootImpl <em>Document Root</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.ide.impl.DocumentRootImpl
	 * @see org.mule.ide.impl.MuleIDEPackageImpl#getDocumentRoot()
	 * @generated
	 */
	int DOCUMENT_ROOT = 3;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__MIXED = 0;

	/**
	 * The feature id for the '<em><b>XMLNS Prefix Map</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__XMLNS_PREFIX_MAP = 1;

	/**
	 * The feature id for the '<em><b>XSI Schema Location</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__XSI_SCHEMA_LOCATION = 2;

	/**
	 * The feature id for the '<em><b>Mule Ide Config</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__MULE_IDE_CONFIG = 3;

	/**
	 * The number of structural features of the the '<em>Document Root</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.ide.impl.MuleIdeConfigTypeImpl <em>Mule Ide Config Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.ide.impl.MuleIdeConfigTypeImpl
	 * @see org.mule.ide.impl.MuleIDEPackageImpl#getMuleIdeConfigType()
	 * @generated
	 */
	int MULE_IDE_CONFIG_TYPE = 4;

	/**
	 * The feature id for the '<em><b>Config File</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_IDE_CONFIG_TYPE__CONFIG_FILE = 0;

	/**
	 * The feature id for the '<em><b>Config Set</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_IDE_CONFIG_TYPE__CONFIG_SET = 1;

	/**
	 * The number of structural features of the the '<em>Mule Ide Config Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_IDE_CONFIG_TYPE_FEATURE_COUNT = 2;


	/**
	 * Returns the meta object for class '{@link org.mule.ide.ConfigFileRefType <em>Config File Ref Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Config File Ref Type</em>'.
	 * @see org.mule.ide.ConfigFileRefType
	 * @generated
	 */
	EClass getConfigFileRefType();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.ide.ConfigFileRefType#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see org.mule.ide.ConfigFileRefType#getId()
	 * @see #getConfigFileRefType()
	 * @generated
	 */
	EAttribute getConfigFileRefType_Id();

	/**
	 * Returns the meta object for class '{@link org.mule.ide.ConfigFileType <em>Config File Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Config File Type</em>'.
	 * @see org.mule.ide.ConfigFileType
	 * @generated
	 */
	EClass getConfigFileType();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.ide.ConfigFileType#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.mule.ide.ConfigFileType#getDescription()
	 * @see #getConfigFileType()
	 * @generated
	 */
	EAttribute getConfigFileType_Description();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.ide.ConfigFileType#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see org.mule.ide.ConfigFileType#getId()
	 * @see #getConfigFileType()
	 * @generated
	 */
	EAttribute getConfigFileType_Id();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.ide.ConfigFileType#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see org.mule.ide.ConfigFileType#getPath()
	 * @see #getConfigFileType()
	 * @generated
	 */
	EAttribute getConfigFileType_Path();

	/**
	 * Returns the meta object for class '{@link org.mule.ide.ConfigSetType <em>Config Set Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Config Set Type</em>'.
	 * @see org.mule.ide.ConfigSetType
	 * @generated
	 */
	EClass getConfigSetType();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.ide.ConfigSetType#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.mule.ide.ConfigSetType#getDescription()
	 * @see #getConfigSetType()
	 * @generated
	 */
	EAttribute getConfigSetType_Description();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.ide.ConfigSetType#getConfigFileRef <em>Config File Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Config File Ref</em>'.
	 * @see org.mule.ide.ConfigSetType#getConfigFileRef()
	 * @see #getConfigSetType()
	 * @generated
	 */
	EReference getConfigSetType_ConfigFileRef();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.ide.ConfigSetType#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see org.mule.ide.ConfigSetType#getId()
	 * @see #getConfigSetType()
	 * @generated
	 */
	EAttribute getConfigSetType_Id();

	/**
	 * Returns the meta object for class '{@link org.mule.ide.DocumentRoot <em>Document Root</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Document Root</em>'.
	 * @see org.mule.ide.DocumentRoot
	 * @generated
	 */
	EClass getDocumentRoot();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.ide.DocumentRoot#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.ide.DocumentRoot#getMixed()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EAttribute getDocumentRoot_Mixed();

	/**
	 * Returns the meta object for the map '{@link org.mule.ide.DocumentRoot#getXMLNSPrefixMap <em>XMLNS Prefix Map</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>XMLNS Prefix Map</em>'.
	 * @see org.mule.ide.DocumentRoot#getXMLNSPrefixMap()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EReference getDocumentRoot_XMLNSPrefixMap();

	/**
	 * Returns the meta object for the map '{@link org.mule.ide.DocumentRoot#getXSISchemaLocation <em>XSI Schema Location</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>XSI Schema Location</em>'.
	 * @see org.mule.ide.DocumentRoot#getXSISchemaLocation()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EReference getDocumentRoot_XSISchemaLocation();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.ide.DocumentRoot#getMuleIdeConfig <em>Mule Ide Config</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Mule Ide Config</em>'.
	 * @see org.mule.ide.DocumentRoot#getMuleIdeConfig()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EReference getDocumentRoot_MuleIdeConfig();

	/**
	 * Returns the meta object for class '{@link org.mule.ide.MuleIdeConfigType <em>Mule Ide Config Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Mule Ide Config Type</em>'.
	 * @see org.mule.ide.MuleIdeConfigType
	 * @generated
	 */
	EClass getMuleIdeConfigType();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.ide.MuleIdeConfigType#getConfigFile <em>Config File</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Config File</em>'.
	 * @see org.mule.ide.MuleIdeConfigType#getConfigFile()
	 * @see #getMuleIdeConfigType()
	 * @generated
	 */
	EReference getMuleIdeConfigType_ConfigFile();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.ide.MuleIdeConfigType#getConfigSet <em>Config Set</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Config Set</em>'.
	 * @see org.mule.ide.MuleIdeConfigType#getConfigSet()
	 * @see #getMuleIdeConfigType()
	 * @generated
	 */
	EReference getMuleIdeConfigType_ConfigSet();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	MuleIDEFactory getMuleIDEFactory();

} //MuleIDEPackage
