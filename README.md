#Keycloak Group Select Extension

##Purpose
The purpose of this extension is to add another form after initial login where a user is forced to select one of their groups that matches a configurable regular expression. Once selected this is pushed into a configurable user attribute with their groups that didn't match the expression that can be then mapped into the user claim as desired.

##Setup
1. Build the java project with gradle. `gradle build`
2. Deploy the resulting jar (./bin/lib/keycloakgroupextension.jar) by placing it in the standalone/deployments folder of your keycloak install.
3. Deploy the ftl (./group-question.ftl) by placing it in themes/base/login/ folder of your keycloak install.
4. in the keycloak admin for the desired realm(s).
    1. Navigate to Authentication
    2. Copy the existing browser flow to a new flow
    3. In the forms section of your new flow, click "Actions" Then "Add Execution"
    4. Select "Group Question"
    5. On the page of your new flow click "Actions" to the side of group question then "Config"
    6. Enter anything desired for the alias and fill in the rest of the values as make sense for your application.

