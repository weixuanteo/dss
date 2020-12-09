package eu.europa.esig.dss.jades.validation;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.FoundCertificatesProxy;
import eu.europa.esig.dss.diagnostic.FoundRevocationsProxy;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.enumerations.CertificateOrigin;
import eu.europa.esig.dss.enumerations.CertificateRefOrigin;
import eu.europa.esig.dss.enumerations.TimestampType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JAdESWithValidationDataTstsInvalidTest extends AbstractJAdESTestValidation {

    @Override
    protected DSSDocument getSignedDocument() {
        return new FileDocument("src/test/resources/validation/jades-with-sigAndRefsTst-without-dot.json");
    }

    @Override
    protected void checkTimestamps(DiagnosticData diagnosticData) {
        assertEquals(4, diagnosticData.getTimestampList().size());

        int sigTstCounter = 0;
        int sigAndRfsTstCounter = 0;
        int rfsTstCounter = 0;
        for (TimestampWrapper timestampWrapper : diagnosticData.getTimestampList()) {
            if (TimestampType.SIGNATURE_TIMESTAMP.equals(timestampWrapper.getType())) {
                assertTrue(timestampWrapper.isMessageImprintDataFound());
                assertTrue(timestampWrapper.isMessageImprintDataIntact());
                ++sigTstCounter;
            } else if (TimestampType.VALIDATION_DATA_TIMESTAMP.equals(timestampWrapper.getType())) {
                assertTrue(timestampWrapper.isMessageImprintDataFound());
                assertFalse(timestampWrapper.isMessageImprintDataIntact());
                ++sigAndRfsTstCounter;
            } else if (TimestampType.VALIDATION_DATA_REFSONLY_TIMESTAMP.equals(timestampWrapper.getType())) {
                assertTrue(timestampWrapper.isMessageImprintDataFound());
                assertTrue(timestampWrapper.isMessageImprintDataIntact());
                ++rfsTstCounter;
            }
        }
        assertEquals(1, sigTstCounter);
        assertEquals(2, sigAndRfsTstCounter);
        assertEquals(1, rfsTstCounter);
    }

    @Override
    protected void checkOrphanTokens(DiagnosticData diagnosticData) {
        SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());

        FoundCertificatesProxy foundCertificates = signature.foundCertificates();
        assertEquals(2, foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.KEY_INFO).size());
        assertEquals(3, foundCertificates
                .getRelatedCertificatesByRefOrigin(CertificateRefOrigin.COMPLETE_CERTIFICATE_REFS).size());
        assertEquals(1, foundCertificates
                .getRelatedCertificatesByRefOrigin(CertificateRefOrigin.ATTRIBUTE_CERTIFICATE_REFS).size());

        FoundRevocationsProxy foundRevocations = signature.foundRevocations();
        assertEquals(0, foundRevocations.getRelatedRevocationData().size());
        assertEquals(2, foundRevocations.getOrphanRevocationRefs().size());
    }

}
