package uk.gov.ida.notification.saml.deprecate;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.security.validators.issuer.IssuerValidator;

import java.util.Arrays;
import java.util.List;

public class IdentityProviderAssertionValidator extends AssertionValidator {

    private final AssertionSubjectConfirmationValidator subjectConfirmationValidator;

    public IdentityProviderAssertionValidator(
            IssuerValidator issuerValidator,
            AssertionSubjectValidator subjectValidator,
            AssertionAttributeStatementValidator assertionAttributeStatementValidator,
            AssertionSubjectConfirmationValidator subjectConfirmationValidator) {

        super(issuerValidator, subjectValidator, assertionAttributeStatementValidator, subjectConfirmationValidator);

        this.subjectConfirmationValidator = subjectConfirmationValidator;
    }

    public void validateConsistency(Assertion authnStatementAssertion, Assertion matchingDatasetAssertion) {
        validateConsistency(Arrays.asList(authnStatementAssertion, matchingDatasetAssertion));
    }

    public void validateConsistency(List<Assertion> assertions) {
        ensurePidsMatch(assertions);

        ensureIssuersMatch(assertions);
    }

    private void ensurePidsMatch(List<Assertion> assertions) {
        boolean pidsDoNotMatch = assertions.stream()
            .map(assertion -> assertion.getSubject().getNameID().getValue())
            .distinct()
            .count() > 1;

        if (pidsDoNotMatch) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.mismatchedPersistentIdentifiers();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }

    private void ensureIssuersMatch(List<Assertion> assertions) {
        boolean issuerValuesDoNotMatch = assertions.stream()
            .map(assertion -> assertion.getIssuer().getValue())
            .distinct()
            .count() > 1;

        if (issuerValuesDoNotMatch) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.mismatchedIssuers();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }

    @Override
    protected void validateSubject(
            Assertion assertion,
            String requestId,
            String expectedRecipientId) {

        super.validateSubject(assertion, requestId, expectedRecipientId);

        ensurePresenceOfBearerSubjectConfirmation(assertion);

        validateAllBearerSubjectConfirmations(assertion, requestId);
    }

    private void validateAllBearerSubjectConfirmations(
            Assertion assertion,
            String requestId) {

        for (SubjectConfirmation subjectConfirmation : assertion.getSubject().getSubjectConfirmations()) {
            if (SubjectConfirmation.METHOD_BEARER.equals(subjectConfirmation.getMethod())) {
                subjectConfirmationValidator.validate(subjectConfirmation, requestId);
            }
        }
    }

    private void ensurePresenceOfBearerSubjectConfirmation(Assertion assertion) {
        boolean hasSubjectConfirmationWithBearerMethod = false;
        for (SubjectConfirmation subjectConfirmation : assertion.getSubject().getSubjectConfirmations()) {
            if (SubjectConfirmation.METHOD_BEARER.equals(subjectConfirmation.getMethod())) {
                hasSubjectConfirmationWithBearerMethod = true;
            }
        }

        if (!hasSubjectConfirmationWithBearerMethod) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.noSubjectConfirmationWithBearerMethod(assertion.getID());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }

}
