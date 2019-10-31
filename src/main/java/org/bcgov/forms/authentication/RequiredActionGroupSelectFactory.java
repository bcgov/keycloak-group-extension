package org.bcgov.forms.authentication;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author Brandon Sharratt
 */
public class RequiredActionGroupSelectFactory implements RequiredActionFactory {

    private static final RequiredActionGroupSelect SINGLETON = new RequiredActionGroupSelect();

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return SINGLETON;
    }


    @Override
    public String getId() {
        return RequiredActionGroupSelect.PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Group Question";
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

}




