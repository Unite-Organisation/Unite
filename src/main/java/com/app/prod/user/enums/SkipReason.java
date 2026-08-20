package com.app.prod.user.enums;

public enum SkipReason {
    /**
     * Somebody already uses this address on an active account.
     */
    EMAIL_ALREADY_USED,
    /**
     * The account is still waiting for activation and the invitation did reach the recipient, so
     * there is nothing to repeat - as opposed to an invitation that never arrived, which is re-sent.
     */
    INVITATION_ALREADY_SENT,
    DUPLICATED_IN_REQUEST
}
