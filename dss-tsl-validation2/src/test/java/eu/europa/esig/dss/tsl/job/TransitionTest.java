package eu.europa.esig.dss.tsl.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.service.http.commons.DSSFileLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.tsl.cache.state.CacheStateEnum;
import eu.europa.esig.dss.tsl.source.TLSource;
import eu.europa.esig.dss.tsl.summary.TLInfo;
import eu.europa.esig.dss.tsl.summary.ValidationJobSummary;

public class TransitionTest {

	private DSSDocument CZ = new FileDocument("src/test/resources/lotlCache/CZ.xml");
	private DSSDocument CZ_NULL = null;
	private DSSDocument CZ_NO_XML = new FileDocument("src/test/resources/lotlCache/CZ.pdf");
	private DSSDocument CZ_BROKEN_SIG = new FileDocument("src/test/resources/lotlCache/CZ_broken-sig.xml");
	private DSSDocument CZ_NO_SIG = new FileDocument("src/test/resources/lotlCache/CZ_no-sig.xml");
	private DSSDocument CZ_NOT_CONFORM = new FileDocument("src/test/resources/lotlCache/CZ_not-conform.xml");
	private DSSDocument CZ_NOT_COMPLIANT = new FileDocument("src/test/resources/lotlCache/CZ_not-compliant.xml");

	@Test
	public void nullDoc() {

		String url = "null-doc";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ_NULL, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.ERROR, CacheStateEnum.REFRESH_NEEDED, CacheStateEnum.REFRESH_NEEDED);

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.ERROR, CacheStateEnum.REFRESH_NEEDED, CacheStateEnum.REFRESH_NEEDED);
	}

	@Test
	public void nullToValidDoc() {

		String url = "null-to-valid-doc";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ_NULL, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.ERROR, CacheStateEnum.REFRESH_NEEDED, CacheStateEnum.REFRESH_NEEDED);

		job.setOnlineDataLoader(getOnlineDataLoader(CZ, url));
		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED);
	}

	@Test
	public void validToNulldDoc() {

		String url = "valid-to-null-doc";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED);

		job.setOnlineDataLoader(getOnlineDataLoader(CZ_NULL, url));
		job.onlineRefresh();

		// valid parsing and signature are still present
		checkSummary(job.getSummary(), CacheStateEnum.ERROR, CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED);
	}

	@Test
	public void validToNonCompliantDoc() {

		String url = "valid-to-null-doc";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED);
		// TODO sync first
		// job.setOnlineDataLoader(getOnlineDataLoader(CZ_NOT_COMPLIANT, url));
		// job.onlineRefresh();
		// checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED,
		// CacheStateEnum.ERROR, CacheStateEnum.DESYNCHRONIZED);
	}

	@Test
	public void nullToNonCompliantAndThenValidDoc() {

		String url = "null-to-valid-doc";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));

		job.setOnlineDataLoader(getOnlineDataLoader(CZ_NULL, url));
		job.onlineRefresh();
		checkSummary(job.getSummary(), CacheStateEnum.ERROR, CacheStateEnum.REFRESH_NEEDED, CacheStateEnum.REFRESH_NEEDED);

		job.setOnlineDataLoader(getOnlineDataLoader(CZ_NOT_COMPLIANT, url));
		job.onlineRefresh();
		checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.ERROR, CacheStateEnum.DESYNCHRONIZED);

		// TODO sync first
		// job.setOnlineDataLoader(getOnlineDataLoader(CZ, url));
		// job.onlineRefresh();
		// checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED,
		// CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED);
	}

	@Test
	public void noXml() {

		String url = "no-xml";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ_NO_XML, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.ERROR, CacheStateEnum.REFRESH_NEEDED, CacheStateEnum.REFRESH_NEEDED);
	}

	@Test
	public void notConform() {

		String url = "no-conform";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ_NOT_CONFORM, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.ERROR, CacheStateEnum.REFRESH_NEEDED, CacheStateEnum.REFRESH_NEEDED);
	}

	@Test
	public void notCompliant() {

		String url = "no-compliant";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ_NOT_COMPLIANT, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.ERROR, CacheStateEnum.DESYNCHRONIZED);
	}

	@Test
	public void validDoc() {

		String url = "valid-doc";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED);
	}

	@Test
	public void noSig() {

		String url = "no-sig";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ_NO_SIG, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED);
	}

	@Test
	public void brokenSig() {

		String url = "broken-sig";

		TLValidationJob job = new TLValidationJob();
		job.setTrustedListSources(getTLSource(url));
		job.setOnlineDataLoader(getOnlineDataLoader(CZ_BROKEN_SIG, url));

		job.onlineRefresh();

		checkSummary(job.getSummary(), CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED, CacheStateEnum.DESYNCHRONIZED);
	}

	private TLSource getTLSource(String url) {
		TLSource czTLSource = new TLSource();
		czTLSource.setUrl(url);
		CertificateSource certificateSource = new CommonCertificateSource();
		certificateSource.addCertificate(DSSUtils.loadCertificateFromBase64EncodedString(
				"MIIISDCCBjCgAwIBAgIEAK+KyjANBgkqhkiG9w0BAQsFADB/MQswCQYDVQQGEwJDWjEoMCYGA1UEAwwfSS5DQSBRdWFsaWZpZWQgMiBDQS9SU0EgMDIvMjAxNjEtMCsGA1UECgwkUHJ2bsOtIGNlcnRpZmlrYcSNbsOtIGF1dG9yaXRhLCBhLnMuMRcwFQYDVQQFEw5OVFJDWi0yNjQzOTM5NTAeFw0xOTAzMDQwOTQzMThaFw0yMDAzMDMwOTQzMThaMIGiMR0wGwYDVQQDDBRJbmcuIFJhZG9tw61yIMWgaW1lazERMA8GA1UEKgwIUmFkb23DrXIxDzANBgNVBAQMBsWgaW1lazELMAkGA1UEBhMCQ1oxNzA1BgNVBAoMLk1pbmlzdHJ5IG9mIHRoZSBJbnRlcmlvciBvZiB0aGUgQ3plY2ggUmVwdWJsaWMxFzAVBgNVBAUTDklDQSAtIDEwNDkzOTg5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj0NF1nqVxU2B/ZO2MKuO6MYN6qH5SGntLvtAAFTYJXyiafT6zzSBXhHHW0bvVMsfW/GGeyVKfrDzz9J+Aw45UbC7+tDkQ+3AGqYpM9y2WhSqw4dsZSNm9Qz/Jrw7HSe7wrEJeg4X0vjXU0jt8Kh1hq5Sz1tEvbhLU9sTCRBnkS5a9ZeGfSJNpOLLowQQZ/HiHjgVMVcm576ij1jo1mGYz5304e+nIkl1IC8EbIrwe+is1LhMxcqMBooEVdb/ZjaA/7Q/3KESgErXbYMitmFQ0OdH6fEKx+uerw/KO7wExDY0RbbsyEbLWOTuzQQfH+lqZJOF3Dl8Ey9n6QrverDA5QIDAQABo4IDpjCCA6IwVQYDVR0RBE4wTIEVcmFkb21pci5zaW1la0BtdmNyLmN6oBgGCisGAQQBgbhIBAagCgwIMTA0OTM5ODmgGQYJKwYBBAHcGQIBoAwMCjE4OTUxNDA4MDgwHwYJYIZIAYb4QgENBBIWEDkyMDMwMzAwMDAwMTEyNzMwDgYDVR0PAQH/BAQDAgbAMAkGA1UdEwQCMAAwggEoBgNVHSAEggEfMIIBGzCCAQwGDSsGAQQBgbhICgEeAQEwgfowHQYIKwYBBQUHAgEWEWh0dHA6Ly93d3cuaWNhLmN6MIHYBggrBgEFBQcCAjCByxqByFRlbnRvIGt2YWxpZmlrb3ZhbnkgY2VydGlmaWthdCBwcm8gZWxla3Ryb25pY2t5IHBvZHBpcyBieWwgdnlkYW4gdiBzb3VsYWR1IHMgbmFyaXplbmltIEVVIGMuIDkxMC8yMDE0LlRoaXMgaXMgYSBxdWFsaWZpZWQgY2VydGlmaWNhdGUgZm9yIGVsZWN0cm9uaWMgc2lnbmF0dXJlIGFjY29yZGluZyB0byBSZWd1bGF0aW9uIChFVSkgTm8gOTEwLzIwMTQuMAkGBwQAi+xAAQIwgY8GA1UdHwSBhzCBhDAqoCigJoYkaHR0cDovL3FjcmxkcDEuaWNhLmN6LzJxY2ExNl9yc2EuY3JsMCqgKKAmhiRodHRwOi8vcWNybGRwMi5pY2EuY3ovMnFjYTE2X3JzYS5jcmwwKqAooCaGJGh0dHA6Ly9xY3JsZHAzLmljYS5jei8ycWNhMTZfcnNhLmNybDCBkgYIKwYBBQUHAQMEgYUwgYIwCAYGBACORgEBMAgGBgQAjkYBBDBXBgYEAI5GAQUwTTAtFidodHRwczovL3d3dy5pY2EuY3ovWnByYXZ5LXByby11eml2YXRlbGUTAmNzMBwWFmh0dHBzOi8vd3d3LmljYS5jei9QRFMTAmVuMBMGBgQAjkYBBjAJBgcEAI5GAQYBMGUGCCsGAQUFBwEBBFkwVzAqBggrBgEFBQcwAoYeaHR0cDovL3EuaWNhLmN6LzJxY2ExNl9yc2EuY2VyMCkGCCsGAQUFBzABhh1odHRwOi8vb2NzcC5pY2EuY3ovMnFjYTE2X3JzYTAfBgNVHSMEGDAWgBR0ggiR49lkaHGF1usx5HLfiyaxbTAdBgNVHQ4EFgQUkVUbJXHGZ+cJtqHZKttyclziLAcwEwYDVR0lBAwwCgYIKwYBBQUHAwQwDQYJKoZIhvcNAQELBQADggIBAJ02rKq039tzkKhCcYWvZVR6ZyRH++kJiVdm0gxmmpjcHo37A2sDFkjt19v2WpDtTMswVoBKE1Vpo+GN19WxNixAxfZLP8NJRdeopvr1m05iBdmzfIuOZ7ehb6g8xVSoC9BEDDzGIXHJaVDv60sr4E80RNquD3UHia1O0V4CQk/bY1645/LETBqGopeZUAPJcdqSj342ofR4iXTOOwl7hl7qEbNKefSzEnEKSHLqnBomi4kUqT7d5zFJRxI8fS6esfqNi74WS0dofHNxh7sf8F7m7F6lsEkXNrcD84OQg+NU00km92ATaRp4dLS79KSkSPH5Jv3oOkmZ8epjNoA6b9lBAZH9ZL8HlwF7gYheg+jfYmXAeMu6vAeXXVJyi7QaMVawkGLNJsn9gTCw7B55dT/XL8yyAia2aSUj1mRogWzYBQbvC5fPxAvRyweikTwPRngVNSHN85ed/NnLAKDpTlOrJhGoRltm2d7xWa5/AJCZP91Yr//Dex8mksslyYU9yB5tP4ZZrVBRjR4KX8DOMO3rf+R9rJFEMefsAkgwOFeJ5VjXof3QGjy7sHxlVG+dG4xFEvuup7Dt6kFHuVxNxwJVZ+umfgteZcGtrucKgw0Nh4fv4ixOfez6UOZpkCdCmjg1AlLSnEhERb2OGCMVSdAu9mHsINNDhRDhoDBYOxyn"));
		czTLSource.setCertificateSource(certificateSource);
		return czTLSource;
	}

	private DSSFileLoader getOnlineDataLoader(DSSDocument doc, String url) {
		FileCacheDataLoader onlineFileLoader = new FileCacheDataLoader();
		onlineFileLoader.setCacheExpirationTime(0);
		Map<String, DSSDocument> onlineMap = new HashMap<String, DSSDocument>();
		onlineMap.put(url, doc);
		onlineFileLoader.setDataLoader(new MockDataLoader(onlineMap));
		onlineFileLoader.setFileCacheDirectory(new File("target/cz-cache"));
		return onlineFileLoader;
	}

	private void checkSummary(ValidationJobSummary summary, CacheStateEnum download, CacheStateEnum parsing, CacheStateEnum validation) {
		assertNotNull(summary);
		List<TLInfo> tlInfos = summary.getTLInfos();
		assertEquals(1, tlInfos.size());
		TLInfo tlInfo = tlInfos.get(0);
		assertEquals(download, tlInfo.getDownloadJobState());
		assertEquals(parsing, tlInfo.getParsingJobState());
		assertEquals(validation, tlInfo.getValidationJobState());
	}

}
