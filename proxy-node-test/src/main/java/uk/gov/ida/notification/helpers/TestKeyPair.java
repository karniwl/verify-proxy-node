package uk.gov.ida.notification.helpers;

import io.dropwizard.testing.ResourceHelpers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class TestKeyPair {

    private static final String X509 = "X.509";
    private static final String RSA = "RSA";
    private static final String TEST_CERTIFICATE_FILE = "test_certificate.crt";
    private static final String TEST_PRIVATE_KEY_FILE = "test_private_key.pk8";

    public final X509Certificate certificate;
    public final PublicKey publicKey;
    public final PrivateKey privateKey;

    public TestKeyPair() throws
            CertificateException,
            IOException,
            NoSuchAlgorithmException,
            InvalidKeySpecException {
        this(TEST_CERTIFICATE_FILE, TEST_PRIVATE_KEY_FILE);
    }

    public TestKeyPair(String certFile, String keyFile) throws
            CertificateException,
            IOException,
            NoSuchAlgorithmException,
            InvalidKeySpecException {
        certificate = readX509Certificate(certFile);
        publicKey = certificate.getPublicKey();
        privateKey = readPrivateKey(keyFile);
    }

    public String getEncodedCertificate() throws CertificateEncodingException {
        return Base64.getEncoder().encodeToString(certificate.getEncoded());
    }

    private X509Certificate readX509Certificate(String certificateFile) throws CertificateException, IOException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance(X509);
        byte[] cert = FileHelpers.readFileAsBytes(certificateFile);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cert);
        return (X509Certificate) certificateFactory.generateCertificate(byteArrayInputStream);
    }

    private PrivateKey readPrivateKey(String privateKeyFile) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        byte bytes[] = FileHelpers.readFileAsBytes(privateKeyFile);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }
}
