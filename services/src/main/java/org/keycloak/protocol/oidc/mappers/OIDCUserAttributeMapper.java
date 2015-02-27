package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappings UserModel.attribute to an ID Token claim.  Token claim name can be a full qualified nested object name,
 * i.e. "address.country".  This will create a nested
 * json object within the toke claim.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCUserAttributeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();
    public static final String USER_MODEL_ATTRIBUTE_NAME = "UserModel Attribute Name";

    static {
        ConfigProperty property;
        property = new ConfigProperty();
        property.setName(USER_MODEL_ATTRIBUTE_NAME);
        property.setLabel(USER_MODEL_ATTRIBUTE_NAME);
        property.setHelpText("Name of stored user attribute which is the name of an attribute within the UserModel.attribute map.");
        configProperties.add(property);
        property = new ConfigProperty();
        property.setName(AttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setLabel(AttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setHelpText("Name of the claim to insert into the token.  This can be a fully qualified name like 'address.street'.  In this case, a nested json object will be created.");
        configProperties.add(property);

    }

    public static final String PROVIDER_ID = "oidc-usermodel-attribute-mapper";


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Attribute";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a custom user attribute to a token claim.";
    }

    @Override
    public AccessToken transformToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        String attributeName = mappingModel.getConfig().get(USER_MODEL_ATTRIBUTE_NAME);
        String attributeValue = user.getAttribute(attributeName);
        if (attributeValue == null) return token;
        AttributeMapperHelper.mapClaim(token, mappingModel, attributeValue);
        return token;
    }

}