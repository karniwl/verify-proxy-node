package uk.gov.ida.notification.stubconnector.resources;

import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import se.litsec.eidas.opensaml.ext.attributes.EidasAttributeValueType;
import se.litsec.opensaml.common.validation.CoreValidatorParameters;
import se.litsec.opensaml.saml2.common.response.ResponseValidator;
import uk.gov.ida.notification.saml.ResponseAssertionDecrypter;
import uk.gov.ida.notification.saml.SamlFormMessageType;
import uk.gov.ida.notification.stubconnector.StubConnectorConfiguration;
import uk.gov.ida.notification.stubconnector.views.ResponseView;
import uk.gov.ida.saml.metadata.bundle.MetadataResolverBundle;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/SAML2/Response")
public class ReceiveResponseResource {
    private final ResponseAssertionDecrypter decrypter;

    private ResponseValidator responseValidator;

    private MetadataResolverBundle<StubConnectorConfiguration> connectorMetadataResolverBundle;

    public ReceiveResponseResource(ResponseAssertionDecrypter decrypter) {
        this.decrypter = decrypter;
    }

    @POST
    @Path("/POST")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseView receiveResponse(
            @FormParam(SamlFormMessageType.SAML_RESPONSE) Response response,
            @FormParam("RelayState") String relayState) {

        System.out.println("RelayState");
        connectorMetadataResolverBundle = new MetadataResolverBundle<>(StubConnectorConfiguration::getProxyNodeMetadataConfiguration);
        SAMLSignatureProfileValidator samlSignatureProfileValidator = new SAMLSignatureProfileValidator();
        responseValidator = new ResponseValidator(connectorMetadataResolverBundle.getSignatureTrustEngine(), samlSignatureProfileValidator);
        System.out.println("ResponseValidator");
        ValidationContext validationContext = new ValidationContext(buildStaticParemeters());
        ValidationResult validate = responseValidator.validate(response, validationContext);

       switch (validate) {
           case VALID: System.out.println("valid");

           case INVALID: System.out.println("invalid");

           case INDETERMINATE: System.out.println("indeterminate");
       }

        // The eIDAS Response should only contain one Assertion with one AttributeStatement which contains
        // the user's requested attributes
        Assertion assertion = decrypter.decrypt(response).getAssertions().get(0);
        AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
        List<String> attributes = attributeStatement.getAttributes().stream()
                .map(attr -> ((EidasAttributeValueType) attr.getAttributeValues().get(0)).toStringValue())
                .collect(Collectors.toList());

        return new ResponseView(attributes);
    }

    private Map<String,Object> buildStaticParemeters() {
        HashMap<String, Object> params = new HashMap<>();

        params.put(CoreValidatorParameters.SIGNATURE_REQUIRED, true);

        return params;
    }
}
