package com.app.prod.exceptions;

import lombok.Getter;

public enum Code {
    CONVERSATION_NOT_FOUND("conversation_not_found", "Conversation not found"),
    POST_NOT_FOUND("post_not_found", "Post not found"),
    AREA_NOT_FOUND("area_not_found", "Area not found"),
    FACILITY_NOT_FOUND("facility_not_found", "Facility not found"),
    NOTIFICATION_NOT_FOUND("notification_not_found", "Notification not found"),
    BUILDING_NOT_FOUND("building_not_found", "Building not found"),
    POLL_NOT_FOUND("poll_not_found", "Poll not found"),
    REQUEST_NOT_FOUND("request_not_found", "Request not found"),
    OFFERING_NOT_FOUND("offering_not_found", "Offering not found"),
    OFFERING_ALREADY_CLOSED("offering_closed", "Offering is finished"),
    ISSUE_NOT_FOUND("issue_not_found", "Issue not found"),
    USER_ROLE_NOT_FOUND("user_role_not_found", "User role not found"),
    POLL_NOT_FINISHED("poll_not_finished", "Poll is not finished yet"),
    MANAGER_CANNOT_VOTE("manager_cannot_vote", "Manager cannot vote"),
    MANAGER_NO_ACCESS("manager_no_access", "Manager no access"),
    USER_NOT_FOUND("user_not_found", "User not found"),
    BAD_CREDENTIALS("bad_credentials", "Bad credentials"),
    USERNAME_TAKEN("username_taken", "Username is taken"),
    EMAIL_TAKEN("email_taken", "Account with this email already exists"),
    UNKNOWN_ERROR("unknown_error", "Unknown error occurred"),
    VALIDATION_ERROR("validation_error", "Request is not valid"),
    CONFLICTING_FILTERS("conflicting_filters", "Filter parameters exclude each other"),
    MANDATORY_FILTER_MISSING("mandatory_filter_missing", "Mandatory filter value is missing"),
    JWT_TOKEN_EXPIRED("jwt_token_expired", "Authorization token has expired"),
    ACCESS_DENIED("access_denied", "Access denied"),
    BUILDING_ACCESS_DENIED("building_access_denied", "No access to this building"),
    BUILDING_ID_REQUIRED("building_id_required", "Building id is required"),
    USER_WITHOUT_BUILDING("user_without_building", "User is not assigned to any building"),
    USER_WITH_BUILDING("user_with_building", "User is already assigned to the building"),
    PRIVATE_CONVERSATION_EXISTS("private_conversation_exists", "Private conversation already exists"),
    INVALID_TIME_PERIOD("invalid_time_period", "Time period not valid"),
    FACILITY_ALREADY_RESERVED("facility_already_reserved", "Facility is already reserved in this period"),
    ISSUE_STRATEGY_NOT_CHOSEN("issue_strategy_not_chosen", "Issue strategy was not chosen"),
    ISSUE_STATUS_ERROR("issue_status_error", "Status error"),
    EMPTY_FILE("empty_file", "File is empty"),
    FILE_NOT_FOUND("file_not_found", "File not found"),
    INVALID_FILE_EXTENSION("invalid_file_extension", "File extension not valid"),
    UNSUPPORTED_FILE_TYPE("unsupported_file_type", "File type is not supported"),
    FILE_TOO_LARGE("file_too_large", "File is too large"),
    TOO_MANY_FILES("too_many_files", "Too many files"),
    INVALID_FILE_KEY("invalid_file_key", "File key is not valid"),
    REQUEST_ALREADY_HANDLED("request_already_handled", "Request is already handled"),
    REQUEST_DONOR("request_donor", "User is not request donor"),
    INTERACTION_NOT_SUPPORTED("interaction_not_supported", "Interaction is not supported for this entity"),
    EVENT_MAX_ATTENDEES("event_max_attendees", "Reached limit of event attendees"),

    ACTIVATION_TOKEN_INVALID("activation_token_invalid", "Activation link is not valid"),
    ACTIVATION_TOKEN_EXPIRED("activation_token_expired", "Activation link has expired"),
    ACTIVATION_TOKEN_USED("activation_token_used", "Activation link has already been used"),

    //errors
    EVENT_HANDLER_NOT_FOUND("event_handler_not_found", "No handler registered for event"),
    EVENT_HANDLER_DUPLICATED("event_handler_duplicated", "Event is handled by more than one handler"),
    JSON_SERIALIZATION_ERROR("json_serialization_error", "Could not read or write json data"),
    INTERNAL_CONNECTION_ERROR("internal_connection", "Internal connection error"),
    MAIL_SENDING_FAILED("mail_sending_failed", "Mail could not be sent"),
    APP_PROFILE_NOT_FOUND("app_profile_not_found", "App profile not found"),
    REFRESH_TOKEN_ERROR("refresh_token_error", "Refresh token error");

    @Getter
    private final String value;
    @Getter private final String defaultMessage;

    Code(String value, String defaultMessage) {
        this.value = value;
        this.defaultMessage = defaultMessage;
    }
}