// /**
//  * 
//  */
// package org.bcgov.forms.authentication;

// import java.util.ArrayList;
// import java.util.List;

// import javax.ws.rs.core.MultivaluedMap;

// import org.keycloak.authentication.FormAction;
// import org.keycloak.authentication.FormActionFactory;
// import org.keycloak.authentication.FormContext;
// import org.keycloak.authentication.ValidationContext;
// import org.keycloak.models.AuthenticatorConfigModel;
// import org.keycloak.events.Details;
// import org.keycloak.events.Errors;
// import org.keycloak.forms.login.LoginFormsProvider;
// import org.keycloak.Config;
// import org.keycloak.models.AuthenticationExecutionModel.Requirement;
// import org.keycloak.models.AuthenticationExecutionModel;
// import org.keycloak.models.KeycloakSession;
// import org.keycloak.models.KeycloakSessionFactory;
// import org.keycloak.models.RealmModel;
// import org.keycloak.models.UserModel;
// import org.keycloak.models.utils.FormMessage;
// import org.keycloak.provider.ProviderConfigProperty;
// import org.keycloak.services.validation.Validation;
// import org.keycloak.authorization.model.Scope;

// /**
//  * @author Brandon Sharratt
//  *
//  */
// public class GroupSelectFormAction implements FormAction, FormActionFactory {

//     private static final String PROVIDER_ID = "group-select-validation-action";
//     private static Requirement[] REQUIREMENT_CHOICES = { Requirement.REQUIRED, Requirement.DISABLED };

//     private static final String GROUP_RE = "group_re";
//     private static final String GROUP_ATTR = "group_attr";

//     @Override
//     public String getId() {
//         return PROVIDER_ID;
//     }


//     @Override
//     public String getDisplayType() {
//         return "Group/Project Selection";
//     }

//     @Override
//     public String getReferenceCategory() {
//         return null;
//     }

//     @Override
//     public boolean isConfigurable() {
//         return true;
//     }

//     @Override
//     public boolean configuredFor(KeycloakSession k, RealmModel r, UserModel u){
//         return true;
//     }

//     @Override
//     public boolean isUserSetupAllowed() {
//         return true;
//     }

//     @Override
//     public String getHelpText() {
//         return "Validates the selection of one group (of a specifiable type)";
//     }

//     public AuthenticationExecutionModel.Requirement[] getRequirementChoices(){
//         return REQUIREMENT_CHOICES;
//     }

//     /*
//      * (non-Javadoc)
//      * 
//      * @see
//      * org.keycloak.authentication.FormAction#buildPage(org.keycloak.authentication.
//      * FormContext, org.keycloak.forms.login.LoginFormsProvider)
//      */
//     @Override
//     public void buildPage(FormContext context, LoginFormsProvider form) {
//         AuthenticatorConfigModel groupConfig = context.getAuthenticatorConfig();
//         if (groupConfig == null || groupConfig.getConfig() == null
//                 || groupConfig.getConfig().get(GROUP_RE) == null
//                 ) {
//             form.addError(new FormMessage(null, "Group Select Not Configured"));
//             return;
//         }
//     }

//     @Override
//     public boolean requiresUser() {
//         return true;
//     }

//     @Override
//     public List<ProviderConfigProperty> getConfigProperties() {
//         List<ProviderConfigProperty> l = new ArrayList<ProviderConfigProperty>();
//         l.add(new ProviderConfigProperty(GROUP_RE, "group-re.label", "group-re.tooltip", ProviderConfigProperty.STRING_TYPE, null));
//         return l;
//     }

//     @Override
//     public void setRequiredActions(KeycloakSession k,RealmModel r, UserModel u){
//         return;
//     }

    
//     public void close(){}

    
//     public void postInit(KeycloakSessionFactory k){}

//     public void init(Config.Scope config){}

    
//     public FormAction create(KeycloakSession k){
//         return this;
//     }
    
//     @Override
//     public void success(FormContext context) {
//         UserModel user = context.getUser();
        
//         MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
//         String group = formData.getFirst("user.groups");
//         ArrayList<String> groups = new ArrayList<String>();
//         groups.add(group);
        
//         String groupRE = context.getAuthenticatorConfig().getConfig().get(GROUP_RE);
//         String groupAttr = context.getAuthenticatorConfig().getConfig().get(GROUP_ATTR);

//         user.setAttribute(GROUP_ATTR, groups);
//     }

//     @Override
//     public void validate(ValidationContext context) {

//         MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
//         List<FormMessage> errors = new ArrayList<>();

//         context.getEvent().detail(Details.REGISTER_METHOD, "form");

//         if (formData.getFirst("user.group") != "") {
//             errors.add(new FormMessage("user.groups", "No user group selected"));
//         }

//         if (errors.size() > 0) {
//             context.error("Invalid selection");
//             context.validationError(formData, errors);
//             return;

//         } else {
//             context.success();
//         }
//     }

// }