package uk.gov.ida.notification.resources;

import io.dropwizard.jersey.sessions.Session;
import io.dropwizard.views.View;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import uk.gov.ida.notification.GatewayConfiguration;
import uk.gov.ida.notification.HubAuthnRequestGenerator;
import uk.gov.ida.notification.SamlFormViewBuilder;
import uk.gov.ida.notification.exceptions.authnrequest.AuthnRequestException;
import uk.gov.ida.notification.saml.SamlFormMessageType;
import uk.gov.ida.notification.saml.EidasAuthnRequest;
import uk.gov.ida.notification.saml.validation.EidasAuthnRequestValidator;
import uk.gov.ida.notification.views.SamlFormView;
import uk.gov.ida.saml.security.validators.signature.SamlRequestSignatureValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;

@Path("/SAML2/SSO")
public class EidasAuthnRequestResource {
    private static final Logger LOG = Logger.getLogger(EidasAuthnRequestResource.class.getName());

    private GatewayConfiguration configuration;
    private final HubAuthnRequestGenerator hubAuthnRequestGenerator;
    private SamlFormViewBuilder samlFormViewBuilder;
    private EidasAuthnRequestValidator eidasAuthnRequestValidator;
    private SamlRequestSignatureValidator samlRequestSignatureValidator;

    @Context
    HttpServletRequest request;

    public EidasAuthnRequestResource(GatewayConfiguration configuration,
                                     HubAuthnRequestGenerator authnRequestGenerator,
                                     SamlFormViewBuilder samlFormViewBuilder,
                                     EidasAuthnRequestValidator eidasAuthnRequestValidator,
                                     SamlRequestSignatureValidator samlRequestSignatureValidator) {
        this.configuration = configuration;
        this.hubAuthnRequestGenerator = authnRequestGenerator;
        this.samlFormViewBuilder = samlFormViewBuilder;
        this.eidasAuthnRequestValidator = eidasAuthnRequestValidator;
        this.samlRequestSignatureValidator = samlRequestSignatureValidator;
    }

    @GET
    @Path("/Redirect")
    public View handleRedirectBinding(
            @QueryParam(SamlFormMessageType.SAML_REQUEST) AuthnRequest encodedEidasAuthnRequest,
            @QueryParam("RelayState") String relayState) {

        HttpSession session = request.getSession();

        return handleAuthnRequest(encodedEidasAuthnRequest, relayState, session);
    }

    @POST
    @Path("/POST")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public View handlePostBinding(
            @FormParam(SamlFormMessageType.SAML_REQUEST) AuthnRequest encodedEidasAuthnRequest,
            @FormParam("RelayState") String relayState) {

        HttpSession session = request.getSession();

        return handleAuthnRequest(encodedEidasAuthnRequest, relayState, session);
    }

    private View handleAuthnRequest(AuthnRequest authnRequest, String relayState, HttpSession session) {
        try {
            samlRequestSignatureValidator.validate(authnRequest, SPSSODescriptor.DEFAULT_ELEMENT_NAME);
            eidasAuthnRequestValidator.validate(authnRequest);
            EidasAuthnRequest eidasAuthnRequest = EidasAuthnRequest.buildFromAuthnRequest(authnRequest);
            AuthnRequest hubAuthnRequest = hubAuthnRequestGenerator.generate(eidasAuthnRequest);
            session.setAttribute("gateway_authn_id", eidasAuthnRequest.getRequestId());
            return buildSamlFormView(hubAuthnRequest, relayState);
        } catch (Throwable example) {
            throw new AuthnRequestException(example, authnRequest);
        }
    }

    private SamlFormView buildSamlFormView(AuthnRequest hubAuthnRequest, String relayState) {
        String hubUrl = configuration.getHubUrl().toString();
        String submitText = "Post Verify Authn Request to Hub";
        return samlFormViewBuilder.buildRequest(hubUrl, hubAuthnRequest, submitText, relayState);
    }

    private void logAuthnRequestInformation(EidasAuthnRequest eidasAuthnRequest) {
        LOG.info("[eIDAS AuthnRequest] Request ID: " + eidasAuthnRequest.getRequestId());
        LOG.info("[eIDAS AuthnRequest] Issuer: " + eidasAuthnRequest.getIssuer());
        LOG.info("[eIDAS AuthnRequest] Destination: " + eidasAuthnRequest.getDestination());
        LOG.info("[eIDAS AuthnRequest] SPType: " + eidasAuthnRequest.getSpType());
        LOG.info("[eIDAS AuthnRequest] Requested level of assurance: " + eidasAuthnRequest.getRequestedLoa());
        eidasAuthnRequest.getRequestedAttributes()
            .stream()
            .forEach((attr) -> LOG.info("[eIDAS AuthnRequest] Requested attribute: " + attr.getName()));
    }
}
