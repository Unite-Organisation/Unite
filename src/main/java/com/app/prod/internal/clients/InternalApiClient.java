package com.app.prod.internal.clients;

import com.app.prod.internal.InternalErrorHandler;

public interface InternalApiClient {
    String API_KEY_HEADER = "X-Internal-Api-Key";
    InternalErrorHandler ERROR_HANDLER = new InternalErrorHandler();
}
