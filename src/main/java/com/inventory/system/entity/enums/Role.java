package com.inventory.system.entity.enums;

/**
 * User roles for role-based access control.
 * ADMIN: full system access, user management.
 * MANAGER: manage products, customers, invoices, reports.
 * BILLING_CLERK: create invoices and manage customers.
 * INVENTORY_CLERK: manage products and stock adjustments.
 */
public enum Role {
    ADMIN,
    MANAGER,
    BILLING_CLERK,
    INVENTORY_CLERK
}
