package eu.europa.esig.dss.jades.signature.counter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.enumerations.JWSSerializationType;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.enumerations.TimestampType;
import eu.europa.esig.dss.jades.JAdESSignatureParameters;
import eu.europa.esig.dss.jades.signature.JAdESService;
import eu.europa.esig.dss.jades.validation.AbstractJAdESTestValidation;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.reports.Reports;

public class JAdESLevelLTAExtensionForCounterSignedTest extends AbstractJAdESTestValidation {
	
	private JAdESService service;
	private DSSDocument documentToSign;
	private JAdESSignatureParameters signatureParameters;
	private JAdESCounterSignatureParameters counterSignatureParameters;
	
	private String signingAlias;
	
	@BeforeEach
	public void init() {
		documentToSign = new FileDocument(new File("src/test/resources/sample.json"));
		
		service = new JAdESService(getCompleteCertificateVerifier());
		service.setTspSource(getGoodTsa());
		
		signingAlias = GOOD_USER;
		
		signatureParameters = new JAdESSignatureParameters();
		signatureParameters.bLevel().setSigningDate(new Date());
		signatureParameters.setSigningCertificate(getSigningCert());
		signatureParameters.setCertificateChain(getCertificateChain());
		signatureParameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
		signatureParameters.setSignatureLevel(SignatureLevel.JAdES_BASELINE_B);
		signatureParameters.setJwsSerializationType(JWSSerializationType.JSON_SERIALIZATION);
		
		signingAlias = SELF_SIGNED_USER;
		
		counterSignatureParameters = new JAdESCounterSignatureParameters();
		counterSignatureParameters.bLevel().setSigningDate(new Date());
		counterSignatureParameters.setSigningCertificate(getSigningCert());
		counterSignatureParameters.setCertificateChain(getCertificateChain());
		counterSignatureParameters.setSignatureLevel(SignatureLevel.JAdES_BASELINE_B);
		counterSignatureParameters.setJwsSerializationType(JWSSerializationType.FLATTENED_JSON_SERIALIZATION);
	}
	
	@Test
	public void test() throws Exception {
		signingAlias = GOOD_USER;
		
		ToBeSigned dataToSign = service.getDataToSign(documentToSign, signatureParameters);
		SignatureValue signatureValue = getToken().sign(dataToSign, signatureParameters.getDigestAlgorithm(),
				signatureParameters.getMaskGenerationFunction(), getPrivateKeyEntry());
		DSSDocument signedDocument = service.signDocument(documentToSign, signatureParameters, signatureValue);
		
		signingAlias = SELF_SIGNED_USER;
		
		ToBeSigned dataToBeCounterSigned = service.getDataToBeCounterSigned(signedDocument, counterSignatureParameters);
		signatureValue = getToken().sign(dataToBeCounterSigned, counterSignatureParameters.getDigestAlgorithm(),
				counterSignatureParameters.getMaskGenerationFunction(), getPrivateKeyEntry());
		DSSDocument counterSignedSignature = service.counterSignSignature(signedDocument, counterSignatureParameters, signatureValue);
		
		// counterSignedSignature.save("target/counterSignedSignature.json");
		
		signatureParameters = new JAdESSignatureParameters();
		signatureParameters.setSignatureLevel(SignatureLevel.JAdES_BASELINE_LTA);
		signatureParameters.setJwsSerializationType(JWSSerializationType.JSON_SERIALIZATION);

		DSSDocument ltaJAdES = service.extendDocument(counterSignedSignature, signatureParameters);
		
		// ltaJAdES.save("target/ltaJAdES.json");
		
		Reports reports = verify(ltaJAdES);
		DiagnosticData diagnosticData = reports.getDiagnosticData();
		
		List<SignatureWrapper> signatures = diagnosticData.getSignatures();
		assertEquals(2, signatures.size());
		
		SignatureWrapper signatureWrapper = signatures.get(0);
		assertFalse(signatureWrapper.isCounterSignature());
		
		counterSignatureParameters.setSigningSignatureId(signatureWrapper.getId());
		dataToBeCounterSigned = service.getDataToBeCounterSigned(ltaJAdES, counterSignatureParameters);
		assertNotNull(dataToBeCounterSigned); // possible to counter sign again
		
		Set<SignatureWrapper> counterSignatures = diagnosticData.getAllCounterSignatures();
		assertEquals(1, counterSignatures.size());
		SignatureWrapper counterSignature = counterSignatures.iterator().next();
		
		counterSignatureParameters.setSigningSignatureId(counterSignature.getId());
		Exception exception = assertThrows(DSSException.class, () -> service.getDataToBeCounterSigned(ltaJAdES, counterSignatureParameters));
		assertEquals(String.format("Unable to counter sign a signature with Id '%s'. "
				+ "The signature is timestamped by a master signature!", counterSignature.getId()), exception.getMessage());
		
	}
	
	@Override
	protected void checkAdvancedSignatures(List<AdvancedSignature> signatures) {
		super.checkAdvancedSignatures(signatures);
		
		assertEquals(1, signatures.size());
		
		AdvancedSignature advancedSignature = signatures.get(0);
		List<AdvancedSignature> counterSignatures = advancedSignature.getCounterSignatures();
		assertEquals(1, counterSignatures.size());
	}
	
	@Override
	protected void checkTimestamps(DiagnosticData diagnosticData) {
		super.checkTimestamps(diagnosticData);
		
		Set<SignatureWrapper> counterSignatures = diagnosticData.getAllCounterSignatures();
		assertEquals(1, counterSignatures.size());
		SignatureWrapper counterSignature = counterSignatures.iterator().next();
		
		List<TimestampWrapper> timestampList = diagnosticData.getTimestampList();
		assertEquals(2, timestampList.size());
		
		boolean sigTstFound = false;
		boolean arcTstFound = false;
		for (TimestampWrapper timestampWrapper : timestampList) {
			if (TimestampType.SIGNATURE_TIMESTAMP.equals(timestampWrapper.getType())) {
				assertEquals(1, timestampWrapper.getTimestampedSignatures().size());
				assertFalse(timestampWrapper.getTimestampedSignatures().stream().map(s -> s.getId()).collect(Collectors.toList())
						.contains(counterSignature.getId()));
				assertFalse(timestampWrapper.getTimestampedCertificates().stream().map(c -> c.getId()).collect(Collectors.toList())
						.contains(counterSignature.getSigningCertificate().getId()));
				sigTstFound = true;
				
			} else if (TimestampType.ARCHIVE_TIMESTAMP.equals(timestampWrapper.getType())) {
				assertEquals(2, timestampWrapper.getTimestampedSignatures().size());
				assertTrue(timestampWrapper.getTimestampedSignatures().stream().map(s -> s.getId()).collect(Collectors.toList())
						.contains(counterSignature.getId()));
				assertTrue(timestampWrapper.getTimestampedCertificates().stream().map(c -> c.getId()).collect(Collectors.toList())
						.contains(counterSignature.getSigningCertificate().getId()));
				arcTstFound = true;
				
			}
		}
		assertTrue(sigTstFound);
		assertTrue(arcTstFound);
	}
	
	@Override
	public void validate() {
		// do nothing
	}

	@Override
	protected DSSDocument getSignedDocument() {
		return null;
	}
	
	@Override
	protected String getSigningAlias() {
		return signingAlias;
	}

}
