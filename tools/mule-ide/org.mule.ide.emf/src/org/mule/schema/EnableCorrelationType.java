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
 * A representation of the literals of the enumeration '<em><b>Enable Correlation Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage#getEnableCorrelationType()
 * @model
 * @generated
 */
public final class EnableCorrelationType extends AbstractEnumerator {
    /**
     * The '<em><b>ALWAYS</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>ALWAYS</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #ALWAYS_LITERAL
     * @model
     * @generated
     * @ordered
     */
    public static final int ALWAYS = 0;

    /**
     * The '<em><b>NEVER</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>NEVER</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #NEVER_LITERAL
     * @model
     * @generated
     * @ordered
     */
    public static final int NEVER = 1;

    /**
     * The '<em><b>IF NOT SET</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>IF NOT SET</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #IF_NOT_SET_LITERAL
     * @model
     * @generated
     * @ordered
     */
    public static final int IF_NOT_SET = 2;

    /**
     * The '<em><b>ALWAYS</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #ALWAYS
     * @generated
     * @ordered
     */
    public static final EnableCorrelationType ALWAYS_LITERAL = new EnableCorrelationType(ALWAYS, "ALWAYS");

    /**
     * The '<em><b>NEVER</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #NEVER
     * @generated
     * @ordered
     */
    public static final EnableCorrelationType NEVER_LITERAL = new EnableCorrelationType(NEVER, "NEVER");

    /**
     * The '<em><b>IF NOT SET</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #IF_NOT_SET
     * @generated
     * @ordered
     */
    public static final EnableCorrelationType IF_NOT_SET_LITERAL = new EnableCorrelationType(IF_NOT_SET, "IF_NOT_SET");

    /**
     * An array of all the '<em><b>Enable Correlation Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private static final EnableCorrelationType[] VALUES_ARRAY =
        new EnableCorrelationType[] {
            ALWAYS_LITERAL,
            NEVER_LITERAL,
            IF_NOT_SET_LITERAL,
        };

    /**
     * A public read-only list of all the '<em><b>Enable Correlation Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Enable Correlation Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static EnableCorrelationType get(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            EnableCorrelationType result = VALUES_ARRAY[i];
            if (result.toString().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Enable Correlation Type</b></em>' literal with the specified value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static EnableCorrelationType get(int value) {
        switch (value) {
            case ALWAYS: return ALWAYS_LITERAL;
            case NEVER: return NEVER_LITERAL;
            case IF_NOT_SET: return IF_NOT_SET_LITERAL;
        }
        return null;
    }

    /**
     * Only this class can construct instances.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private EnableCorrelationType(int value, String name) {
        super(value, name);
    }

} //EnableCorrelationType
