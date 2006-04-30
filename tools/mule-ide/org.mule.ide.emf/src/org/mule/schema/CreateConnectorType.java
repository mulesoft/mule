/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.AbstractEnumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Create Connector Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage#getCreateConnectorType()
 * @model
 * @generated
 */
public final class CreateConnectorType extends AbstractEnumerator {
    /**
     * The '<em><b>GET OR CREATE</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>GET OR CREATE</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #GET_OR_CREATE_LITERAL
     * @model
     * @generated
     * @ordered
     */
    public static final int GET_OR_CREATE = 0;

    /**
     * The '<em><b>ALWAYS CREATE</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>ALWAYS CREATE</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #ALWAYS_CREATE_LITERAL
     * @model
     * @generated
     * @ordered
     */
    public static final int ALWAYS_CREATE = 1;

    /**
     * The '<em><b>NEVER CREATE</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>NEVER CREATE</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #NEVER_CREATE_LITERAL
     * @model
     * @generated
     * @ordered
     */
    public static final int NEVER_CREATE = 2;

    /**
     * The '<em><b>GET OR CREATE</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #GET_OR_CREATE
     * @generated
     * @ordered
     */
    public static final CreateConnectorType GET_OR_CREATE_LITERAL = new CreateConnectorType(GET_OR_CREATE, "GET_OR_CREATE");

    /**
     * The '<em><b>ALWAYS CREATE</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #ALWAYS_CREATE
     * @generated
     * @ordered
     */
    public static final CreateConnectorType ALWAYS_CREATE_LITERAL = new CreateConnectorType(ALWAYS_CREATE, "ALWAYS_CREATE");

    /**
     * The '<em><b>NEVER CREATE</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #NEVER_CREATE
     * @generated
     * @ordered
     */
    public static final CreateConnectorType NEVER_CREATE_LITERAL = new CreateConnectorType(NEVER_CREATE, "NEVER_CREATE");

    /**
     * An array of all the '<em><b>Create Connector Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private static final CreateConnectorType[] VALUES_ARRAY =
        new CreateConnectorType[] {
            GET_OR_CREATE_LITERAL,
            ALWAYS_CREATE_LITERAL,
            NEVER_CREATE_LITERAL,
        };

    /**
     * A public read-only list of all the '<em><b>Create Connector Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Create Connector Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static CreateConnectorType get(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            CreateConnectorType result = VALUES_ARRAY[i];
            if (result.toString().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Create Connector Type</b></em>' literal with the specified value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static CreateConnectorType get(int value) {
        switch (value) {
            case GET_OR_CREATE: return GET_OR_CREATE_LITERAL;
            case ALWAYS_CREATE: return ALWAYS_CREATE_LITERAL;
            case NEVER_CREATE: return NEVER_CREATE_LITERAL;
        }
        return null;
    }

    /**
     * Only this class can construct instances.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private CreateConnectorType(int value, String name) {
        super(value, name);
    }

} //CreateConnectorType
