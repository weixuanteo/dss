package eu.europa.esig.dss.jades.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestMatcher;
import eu.europa.esig.dss.enumerations.DigestMatcherType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.utils.Utils;

public class JWSSimpleDetachedWithWrongAlgoValidationTest extends AbstractJAdESTestValidation {
	
	@Override
	protected DSSDocument getSignedDocument() {
		return new FileDocument("src/test/resources/validation/simple-detached-wrong-algo.json");
	}
	
	@Override
	protected List<DSSDocument> getDetachedContents() {
		return Arrays.asList(new FileDocument("src/test/resources/sample.json"));
	}

	@Override
	protected void checkBLevelValid(DiagnosticData diagnosticData) {
		SignatureWrapper signatureWrapper = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		assertFalse(signatureWrapper.isBLevelTechnicallyValid());
		assertTrue(signatureWrapper.isSignatureIntact());
		assertFalse(signatureWrapper.isSignatureValid());
	}

	@Override
	protected void checkMessageDigestAlgorithm(DiagnosticData diagnosticData) {
		SignatureWrapper signatureWrapper = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		List<XmlDigestMatcher> digestMatchers = signatureWrapper.getDigestMatchers();
		assertEquals(2, digestMatchers.size());

		XmlDigestMatcher jwsSigningInput = digestMatchers.get(0);
		assertEquals(DigestMatcherType.JWS_SIGNING_INPUT_DIGEST, jwsSigningInput.getType());
		assertNotNull(jwsSigningInput.getDigestMethod());
		assertTrue(Utils.isArrayNotEmpty(jwsSigningInput.getDigestValue()));
		assertTrue(jwsSigningInput.isDataFound());
		assertTrue(jwsSigningInput.isDataIntact());

		XmlDigestMatcher sigDEntry = digestMatchers.get(1);
		assertEquals(DigestMatcherType.SIG_D_ENTRY, sigDEntry.getType());
		assertNotNull(sigDEntry.getName());
		assertNull(sigDEntry.getDigestMethod());
		assertFalse(Utils.isArrayNotEmpty(sigDEntry.getDigestValue()));
		assertTrue(sigDEntry.isDataFound());
		assertFalse(sigDEntry.isDataIntact());
	}

}
