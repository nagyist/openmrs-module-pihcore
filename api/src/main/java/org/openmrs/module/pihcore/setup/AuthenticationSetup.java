package org.openmrs.module.pihcore.setup;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.authentication.AuthenticationConfig;
import org.openmrs.module.pihcore.config.Config;
import org.openmrs.module.pihcore.config.model.AuthenticationConfigDescriptor;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.openmrs.module.authentication.AuthenticationConfig.SCHEME;
import static org.openmrs.module.authentication.AuthenticationConfig.SCHEME_CONFIG_PREFIX_TEMPLATE;
import static org.openmrs.module.authentication.AuthenticationConfig.SCHEME_ID;
import static org.openmrs.module.authentication.AuthenticationConfig.SCHEME_TYPE_TEMPLATE;

public class AuthenticationSetup {

    protected static Log log = LogFactory.getLog(AuthenticationSetup.class);

    public static final String BASIC = "basic";
    public static final String SECRET = "secret";
    public static final String TOTP = "totp";
    public static final String TWO_FACTOR = "2fa";

    /**
     * Setup Authentication Configuration and Schemes
     */
    public static void setup(Config config) {

        // Needed to ensure the authentication module can load custom PIH Authentication Scheme
        AuthenticationConfig.registerClassLoader(AuthenticationSetup.class.getClassLoader());

        // clear out any existing config
        AuthenticationConfig.setConfig(new Properties());

        AuthenticationConfigDescriptor cd = config.getAuthenticationConfig();
        
        // If no authentication scheme is explicitly configured, default to basic
        String scheme = StringUtils.isBlank(cd.getScheme()) ? BASIC : cd.getScheme();
        AuthenticationConfig.setProperty(SCHEME, scheme);

        // Set up all the supported authentication schemes with default values.
        // Allow overriding with values from the config

        // Basic Authentication Scheme.  This provides basic auth + session location selection
        {
            String className = "org.openmrs.module.authentication.web.BasicWithLocationAuthenticationScheme";
            Properties p = new Properties();
            p.put("loginPage", "/authenticationui/login/login.page");
            p.put("usernameParam", "username");
            p.put("passwordParam", "password");
            p.put("locationParamName", "sessionLocation");
            p.put("locationRequired", "true");
            p.put("onlyLocationsWithTag", "Login Location");
            p.put("locationSessionAttributeName", "emrContext.sessionLocationId");
            p.put("lastLocationCookieName", "emr.lastSessionLocation");
            addScheme(BASIC, className, p);
        }

        // Secret Question Authentication Scheme.  This is an available 2nd factor
        {
            String className = "org.openmrs.module.authentication.web.SecretQuestionAuthenticationScheme";
            Properties p = new Properties();
            p.put("loginPage", "/authenticationui/login/loginSecret.page");
            p.put("configurationPage", "/authenticationui/account/changeSecurityQuestion.page?schemeId={schemeId}&userId={userId}");
            addScheme(SECRET, className, p);
        }

        // Totp Authentication Scheme.  This is an available 2nd factor
        {
            String className = "org.openmrs.module.authentication.web.TotpAuthenticationScheme";
            Properties p = new Properties();
            p.put("qrCodeIssuer", "PIHEMR");
            p.put("loginPage", "/authenticationui/login/loginTotp.page");
            p.put("configurationPage", "/authenticationui/account/configureTotp.page?schemeId={schemeId}&userId={userId}");
            addScheme(TOTP, className, p);
        }

        // Two-Factor Authentication Scheme.
        {
            String className = "org.openmrs.module.authentication.web.TwoFactorAuthenticationScheme";
            Properties p = new Properties();
            p.put("primaryOptions", BASIC);
            p.put("secondaryOptions", SECRET + "," + TOTP);
            addScheme(TWO_FACTOR, className, p);
        }

        // Configuration overrides
        for (String schemeId : cd.getSchemes().keySet()) {
            AuthenticationConfigDescriptor.SchemeDescriptor sd = cd.getSchemes().get(schemeId);
            if (StringUtils.isNotBlank(sd.getType())) {
                AuthenticationConfig.setProperty(SCHEME_TYPE_TEMPLATE.replace(SCHEME_ID, schemeId), sd.getType());
            }
            for (String property : sd.getConfig().keySet()) {
                String propertyName = SCHEME_CONFIG_PREFIX_TEMPLATE.replace(SCHEME_ID, schemeId) + property;
                String propertyVal = sd.getConfig().get(property);
                AuthenticationConfig.setProperty(propertyName, propertyVal);
            }
        }

        log.info("Authentication Schemes Configured");
        Properties p = AuthenticationConfig.getConfig();
        Set<String> sortedKeys = new TreeSet<>(p.stringPropertyNames());
        for (String key : sortedKeys) {
            log.info(key + " = " + p.getProperty(key));
        }
    }

    /**
     * Add configuration for a scheme with the given schemeId
     */
    protected static void addScheme(String schemeId, String className, Properties config) {
        String schemeTypeProperty = SCHEME_TYPE_TEMPLATE.replace(SCHEME_ID, schemeId);
        AuthenticationConfig.setProperty(schemeTypeProperty, className);
        if (config != null) {
            for (String propertyName : config.stringPropertyNames()) {
                String key = SCHEME_CONFIG_PREFIX_TEMPLATE.replace(SCHEME_ID, schemeId) + propertyName;
                String value = config.getProperty(propertyName);
                AuthenticationConfig.setProperty(key, value);
            }
        }
    }
}
