package org.bcgov.forms.authentication;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.delegates.ServerCookie;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

/**
 * @author Brandon Sharratt
 *
 */
public class GroupSelectAuthentication implements Authenticator {
    private static final Logger LOG = Logger.getLogger(GroupSelectAuthentication.class.getName());

    final static String GROUP_ATTR_PENDING = "pending";
    final static String GROUP_QUESTION_COOKIE = "GROUP_QUESTION_ANSWERED";
    public static final String CREDENTIAL_TYPE = "group_question";

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;// session.users().configuredForCredentialType("group_question", realm, user);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction("GROUP_QUESTION_CONFIG");
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        //already logged in...skip
        if (hasCookie(context)) {
            context.success();
            return;
        }
        
        // process any pending group requests
        addPendingGroups(context);

        //exclusion group config
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        List<String> exclGr = new ArrayList<String>();
        if (config != null) {
            exclGr = Arrays.asList(String.valueOf(config.getConfig().getOrDefault(GroupSelectAuthenticationFactory.EXCLUSION_PROP, GroupSelectAuthenticationFactory.EXCLUSION_DEFAULT)).split(", "));
        }

        List<String> groups = new ArrayList<String>();

        //group regular expression config
        String groupRe = GroupSelectAuthenticationFactory.EXPRESSION_DEFAULT;
        if (config != null) {
            groupRe = String.valueOf(config.getConfig().getOrDefault(GroupSelectAuthenticationFactory.EXPRESSION_PROP, groupRe));
        }

        Pattern p = Pattern.compile(groupRe);
        Set<GroupModel> groupSet = context.getUser().getGroups();
        ArrayList<String> allGroups = new ArrayList<String>();
        for (Iterator<GroupModel> it = groupSet.iterator(); it.hasNext(); ) {
            GroupModel g = it.next();
            allGroups.add(g.getName());
        }

        for (Iterator<GroupModel> it = groupSet.iterator(); it.hasNext(); ) {
            GroupModel g = it.next();
            
            String fullGroupName = getFullGroupName(g);
            
            Matcher m = p.matcher(fullGroupName);
            //building group list, if the group is among the pattern
            if (m.find()){
                groups.add(fullGroupName);
            }

            //if this group is in the exclusion list, then abort, and add all the groups to the project attribtue
            if (exclGr.contains(fullGroupName)) {
                List<String> project = new ArrayList<String>();
                String projectProp = GroupSelectAuthenticationFactory.ATTRIBUTE_DEFAULT;
                if (config != null) {
                    projectProp = String.valueOf(config.getConfig().getOrDefault(GroupSelectAuthenticationFactory.ATTRIBUTE_PROP, projectProp));
                }
                for (Iterator<GroupModel> it2 = groupSet.iterator(); it2.hasNext(); ) {
                    GroupModel g2 = it2.next();
                    String fullGroupName2 = getFullGroupName(g2);
                    Matcher m2 = p.matcher(fullGroupName2);
                    if (!m2.find()){
                        project.add(fullGroupName2);
                    }
                }

                context.getUser().setAttribute(projectProp, allGroups);
                context.success();
                return;
            }
        }

        //if the user has no groups, then report an error
        if (groups.size() == 0){
            //context.failure(AuthenticationFlowError.CLIENT_DISABLED);

            //used to report an error but that caused undesired side effects
            String projectProp = GroupSelectAuthenticationFactory.ATTRIBUTE_DEFAULT;
            if (config != null) {
                projectProp = String.valueOf(config.getConfig().getOrDefault(GroupSelectAuthenticationFactory.ATTRIBUTE_PROP, projectProp));
            }
            context.getUser().setAttribute(projectProp, new ArrayList<String>());
            context.success();
            return;
        }

        //otherwise challenge them to the group select
        Response challenge = context.form().setAttribute("groups", groups.toArray()).createForm("group-question.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        //check group and report an invalid selection if invalid
        boolean validated = validateAnswer(context);
        if (!validated) {
            Response challenge = context.form().setError("BadGroup").createForm("group-question.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        //otherwise set the cookie and set the user attribute to the selected project & groups
        setCookie(context);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String group = formData.getFirst("group_answer");
        
        List<String> project = new ArrayList<String>();
        project.add(group);

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        String projectProp = GroupSelectAuthenticationFactory.ATTRIBUTE_DEFAULT;
        if (config != null) {
            projectProp = String.valueOf(config.getConfig().getOrDefault(GroupSelectAuthenticationFactory.ATTRIBUTE_PROP, projectProp));
        }

        Set<GroupModel> groupSet = context.getUser().getGroups();
        String groupRe = GroupSelectAuthenticationFactory.EXPRESSION_DEFAULT;
        if (config != null) {
            groupRe = String.valueOf(config.getConfig().getOrDefault(GroupSelectAuthenticationFactory.EXPRESSION_PROP, groupRe));
        }
        //add all the groups that aren't projects
        Pattern p = Pattern.compile(groupRe);
        for (Iterator<GroupModel> it = groupSet.iterator(); it.hasNext(); ) {
            GroupModel g = it.next();
            String fullGroupName = getFullGroupName(g);
            Matcher m = p.matcher(fullGroupName);
            if (!m.find()){
                project.add(fullGroupName);
            }
        }

        context.getUser().setAttribute(projectProp, project);

        context.success();
    }

    protected boolean hasCookie(AuthenticationFlowContext context) {
        Cookie cookie = context.getHttpRequest().getHttpHeaders().getCookies().get(GROUP_QUESTION_COOKIE);
        boolean result = cookie != null;
        if (result) {
            System.out.println("Bypassing group question because cookie as set");
        }
        return result;
    }

    protected void setCookie(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        int maxCookieAge = 60 * 60 * 24 * 30; // 30 days
        if (config != null) {
            maxCookieAge = Integer.valueOf(config.getConfig().getOrDefault("cookie.max.age", String.valueOf(maxCookieAge)));

        }
        URI uri = context.getUriInfo().getBaseUriBuilder().path("realms").path(context.getRealm().getName()).build();
        addCookie("GROUP_QUESTION_COOKIE", "true", uri.getRawPath(), null, null, maxCookieAge, false, true);
    }

    public static void addCookie(String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly) {
        HttpResponse response = ResteasyProviderFactory.getContextData(HttpResponse.class);
        StringBuffer cookieBuf = new StringBuffer();
        ServerCookie.appendCookieValue(cookieBuf, 1, name, value, path, domain, comment, maxAge, secure);
        String cookie = cookieBuf.toString();
        response.getOutputHeaders().add(HttpHeaders.SET_COOKIE, cookie);
    }


    protected boolean validateAnswer(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String group = "";
        try{
            group = formData.getFirst("group_answer");
        }catch(Exception e){
            return false;
        }
        UserCredentialModel input = new UserCredentialModel();
        input.setType(GroupQuestionCredentialProvider.GROUP_QUESTION);

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        String groupRe = GroupSelectAuthenticationFactory.EXPRESSION_DEFAULT;
        if (config != null) {
            groupRe = String.valueOf(config.getConfig().getOrDefault(GroupSelectAuthenticationFactory.EXPRESSION_PROP, groupRe));
        }

        Pattern p = Pattern.compile(groupRe);
        Matcher m = p.matcher(group);

        //make sure the selected group matches the regular expression and is one of the users groups
        if (m.find()){
            Set<GroupModel> groupSet = context.getUser().getGroups();
            for (Iterator<GroupModel> it = groupSet.iterator(); it.hasNext(); ) {
                GroupModel g = it.next();
                String fullGroupName = getFullGroupName(g);
                if (fullGroupName.equals(group)){
                    return true;
                }
            }
        }
        return false;

        
    }

    private String getFullGroupName (GroupModel g) {
        List<String> path = new ArrayList<>();
        GroupModel aNode = g;
        while (aNode != null) {
            path.add(0, aNode.getName());
            if (aNode.getParentId() != null) {
                aNode = aNode.getParent();
            } else {
                aNode = null;
            }
        }
        String fullPath = path.stream().collect(Collectors.joining("/"));

        return path.size() == 1 ? path.get(0) : String.format("/%s", fullPath);
    }
    
    private void addPendingGroups(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        List<GroupModel> groups = context.getRealm().getGroups();
        traverseGroups (groups, user);
    }

    private void traverseGroups (List<GroupModel> groups, UserModel user) {
        String username = user.getUsername();
        for (GroupModel group : groups) {
            boolean processed = false;
            List<String> pending = group.getAttribute(GROUP_ATTR_PENDING);
            if (pending != null) {
                for ( String pendingUser : pending) {
                    if (username.equals(pendingUser)) {
                        user.joinGroup(group);
                        processed = true;
                    }
                }
            }
            if (processed) {
                pending.remove(username);
                group.setAttribute(GROUP_ATTR_PENDING, pending);
            }
            Set<GroupModel> subGroups = group.getSubGroups();
            if (subGroups != null) {
                traverseGroups (new ArrayList<>(group.getSubGroups()), user);
            }
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }
      
    @Override
    public void close() {}

}