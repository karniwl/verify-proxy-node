package uk.gov.ida.notification;

import org.opensaml.saml.saml2.core.AuthnRequest;
import uk.gov.ida.notification.saml.SamlObjectSigner;
import uk.gov.ida.notification.saml.translation.EidasAuthnRequest;
import uk.gov.ida.notification.saml.translation.EidasAuthnRequestTranslator;

public class HubAuthnRequestGenerator {
    private final EidasAuthnRequestTranslator translator;
    private final SamlObjectSigner samlObjectSigner;

    public HubAuthnRequestGenerator(EidasAuthnRequestTranslator translator, SamlObjectSigner samlObjectSigner) {
        this.translator = translator;
        this.samlObjectSigner = samlObjectSigner;
    }

    public AuthnRequest generate(EidasAuthnRequest eidasAuthnRequest) {
        AuthnRequest authnRequest = translator.translate(eidasAuthnRequest);
        return samlObjectSigner.sign(authnRequest);
    }
}
