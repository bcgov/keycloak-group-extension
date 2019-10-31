package org.bcgov.forms.authentication;

import javax.ws.rs.core.Response;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.UserCredentialModel;


/**
 * @author Brandon Sharratt
 *
 */
public class RequiredActionGroupSelect implements RequiredActionProvider {

    public static final String PROVIDER_ID = "group_question_config";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {}

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
       Response challenge = context.form().createForm("group_question_config.ftl");
       context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        String answer = (context.getHttpRequest().getDecodedFormParameters().getFirst("group_answer"));
        UserCredentialModel input = new UserCredentialModel();
        input.setType(GroupQuestionCredentialProvider.GROUP_QUESTION);
        input.setValue(answer);
        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), input);
        context.success();
    }

    @Override
    public void close(){};

}