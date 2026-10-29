package com.app.prod.event.web;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.valves.rewrite.RewriteValve;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * Lets the frontend hosting hand {@code /e/{slug}} over to this backend unchanged.
 * <p>
 * Firebase Hosting forwards the original path to Cloud Run and cannot change it, while everything
 * here lives under the servlet context path ({@code /v1/api}). A request outside of it never reaches
 * the application at all, so it is rewritten by Tomcat itself, before a context is picked:
 * <pre>
 * /e/{slug}             -> /v1/api/public/share/event/{slug}
 * /e/{slug}/preview.png -> /v1/api/public/share/event/{slug}/preview.png
 * /e/{slug}/anything    -> /v1/api/public/share/event/{slug}   (deeper frontend routes get the same page)
 * </pre>
 * The query string is carried over, so the {@code ?v=} of the image survives.
 */
@Component
@RequiredArgsConstructor
public class EventShareRewrite implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    static final String SHARE_PATH = "/public/share/event/";

    private final ServerProperties serverProperties;

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        String contextPath = serverProperties.getServlet().getContextPath();
        String target = (contextPath == null ? "" : contextPath) + SHARE_PATH;

        factory.addEngineValves(new ConfiguredRewriteValve(String.join("\n",
                "RewriteRule ^/e/([A-Za-z0-9_-]+)/preview\\.png$ " + target + "$1/preview.png [L]",
                "RewriteRule ^/e/([A-Za-z0-9_-]+)(/.*)?$ " + target + "$1 [L]"
        )));
    }

    /**
     * An engine level {@link RewriteValve} reads its rules from a {@code rewrite.config} file of the
     * Tomcat base, which embedded Tomcat does not have - the rules are handed over once it started.
     */
    private static class ConfiguredRewriteValve extends RewriteValve {

        private final String rules;

        ConfiguredRewriteValve(String rules) {
            this.rules = rules;
        }

        @Override
        protected void startInternal() throws LifecycleException {
            super.startInternal();
            try {
                setConfiguration(rules);
            } catch (Exception e) {
                throw new LifecycleException("Event share rewrite rules could not be parsed", e);
            }
        }
    }
}
