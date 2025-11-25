package com.app.prod.config.integration;

public class ApiOrigins {
    private static final String FRONTEND_ORIGIN = "http://localhost:";
    private static final String PORT = "4204";

    public static String getFrontendOrigin(){
        return FRONTEND_ORIGIN + PORT;
    }
}
