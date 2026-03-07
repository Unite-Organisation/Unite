package com.app.prod.config.integration;

import java.util.List;

public class ApiOrigins {
    private static final String FRONTEND_ORIGIN = "http://localhost:";
    private static final String FRONTEND_PROD_ORIGIN1 = "https://unite-488210.web.app";
    private static final String FRONTEND_PROD_ORIGIN2 = "https://unite-488210.firebaseapp.com";
    private static final String PORT = "4204";

    public static String getFrontendOrigin(){
        return FRONTEND_ORIGIN + PORT;
    }

    public static List<String> getAllOrigins() {
        return List.of(
                getFrontendOrigin(),
                FRONTEND_PROD_ORIGIN1,
                FRONTEND_PROD_ORIGIN2
        );
    }
}
