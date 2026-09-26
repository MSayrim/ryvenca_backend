package com.ryvenca.deletion;

/** How an account deletion was initiated. */
public enum DeletionMethod {
    /** The user deleted the account in the mobile app. */
    IN_APP,
    /** The user deleted the account on the website. */
    WEB,
    /** An admin approved a deletion request from someone who could not sign in. */
    REQUEST,
    /** An admin deleted the account. */
    ADMIN
}
