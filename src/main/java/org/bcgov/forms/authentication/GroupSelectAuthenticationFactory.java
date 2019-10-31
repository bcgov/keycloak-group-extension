package org.bcgov.forms.authentication;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author Brandon Sharratt
 *
 */
public class GroupSelectAuthenticationFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory{
    public static final String PROVIDER_ID = "group-question-authenticator";
    private static final GroupSelectAuthentication SINGLETON = new GroupSelectAuthentication();
    
    public static final String EXCLUSION_PROP = "user.excl";
    public static final String EXCLUSION_DEFAULT = "";

    public static final String ATTRIBUTE_PROP = "user.prop";
    public static final String ATTRIBUTE_DEFAULT = "projects";

    public static final String EXPRESSION_PROP = "group.re";
    public static final String EXPRESSION_DEFAULT = "project_[.]*";



    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
    };

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }
    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(EXPRESSION_PROP);
        property.setLabel("Group Regular Expression");
        property.setDefaultValue(EXPRESSION_DEFAULT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Regular Expression for the groups to select from");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("User Property");
        property.setLabel(ATTRIBUTE_PROP);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(ATTRIBUTE_DEFAULT);
        property.setHelpText("What property to stash the current project in");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("Exclusion Groups");
        property.setLabel(EXCLUSION_PROP);
        property.setDefaultValue(EXCLUSION_DEFAULT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Groups that if a user is a member of they get to skip the project selection, (seperate multiple values with a comma and a space)");
        configProperties.add(property);
    }

    @Override
    public void close(){}

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getReferenceCategory() {
        return "Group Question";
    }

    @Override
    public String getDisplayType() {
        return "Group Question";
    }

    @Override
    public String getHelpText() {
        return "The user is forced to pick the group that they belong to";
    }


}