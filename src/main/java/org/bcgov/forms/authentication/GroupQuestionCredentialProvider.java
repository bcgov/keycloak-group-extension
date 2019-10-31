package org.bcgov.forms.authentication;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;

/**
 * @author Brandon Sharratt
 */
public class GroupQuestionCredentialProvider implements CredentialProvider, CredentialInputValidator, CredentialInputUpdater, OnUserCache {
    public static final String GROUP_QUESTION = "GROUP_QUESTION";
    public static final String CACHE_KEY = GroupQuestionCredentialProvider.class.getName() + "." + GROUP_QUESTION;

    protected KeycloakSession session;

    public GroupQuestionCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    public CredentialModel getGroup(RealmModel realm, UserModel user) {
        CredentialModel group = null;
        if (user instanceof CachedUserModel) {
            CachedUserModel cached = (CachedUserModel)user;
            group = (CredentialModel)cached.getCachedWith().get(CACHE_KEY);

        } else {
            List<CredentialModel> creds = session.userCredentialManager().getStoredCredentialsByType(realm, user, GROUP_QUESTION);
            if (!creds.isEmpty()) group = creds.get(0);
        }
        return group;
    }


    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!GROUP_QUESTION.equals(input.getType())) return false;
        if (!(input instanceof UserCredentialModel)) return false;
        UserCredentialModel credInput = (UserCredentialModel) input;
        List<CredentialModel> creds = session.userCredentialManager().getStoredCredentialsByType(realm, user, GROUP_QUESTION);
        if (creds.isEmpty()) {
            CredentialModel group = new CredentialModel();
            group.setType(GROUP_QUESTION);
            group.setValue(credInput.getValue());
            group.setCreatedDate(Time.currentTimeMillis());
            session.userCredentialManager().createCredential(realm ,user, group);
        } else {
            creds.get(0).setValue(credInput.getValue());
            session.userCredentialManager().updateCredential(realm, user, creds.get(0));
        }
        session.userCache().evict(realm, user);
        return true;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!GROUP_QUESTION.equals(credentialType)) return;
        session.userCredentialManager().disableCredentialType(realm, user, credentialType);
        session.userCache().evict(realm, user);

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        if (!session.userCredentialManager().getStoredCredentialsByType(realm, user, GROUP_QUESTION).isEmpty()) {
            Set<String> set = new HashSet<>();
            set.add(GROUP_QUESTION);
            return set;
        } else {
            return Collections.EMPTY_SET;
        }

    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return GROUP_QUESTION.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!GROUP_QUESTION.equals(credentialType)) return false;
        return getGroup(realm, user) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!GROUP_QUESTION.equals(input.getType())) return false;
        if (!(input instanceof UserCredentialModel)) return false;

        String group = getGroup(realm, user).getValue();

        return group != null && ((UserCredentialModel)input).getValue().equals(group);
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        List<CredentialModel> creds = session.userCredentialManager().getStoredCredentialsByType(realm, user, GROUP_QUESTION);
        if (!creds.isEmpty()) user.getCachedWith().put(CACHE_KEY, creds.get(0));
    }
}





