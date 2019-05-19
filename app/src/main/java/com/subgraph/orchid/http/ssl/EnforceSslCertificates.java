package com.subgraph.orchid.http.ssl;

import com.subgraph.orchid.Tor;
import com.subgraph.orchid.logging.Logger;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class EnforceSslCertificates {
    private static final Logger logger = Logger.getInstance(EnforceSslCertificates.class);
    private KeyStore KEY_STORE;

    public EnforceSslCertificates() {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            Path ksPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
            keyStore.load(new FileInputStream(ksPath.toFile()), "changeit".toCharArray());

        } catch (Exception e) {
            logger.error("Unable to export ssl certificates.", e);
        } finally {
            KEY_STORE = keyStore;
        }
    }

    public SSLContext getSSLContext() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            if (KEY_STORE == null) {
                KEY_STORE = KeyStore.getInstance(KeyStore.getDefaultType());
                KEY_STORE.load(null, "changeit".toCharArray());
            }
            List<Certificate> a = Tor.getApplication().additionalCertificates();
            for (int i = 0; i < a.size(); i++) {
                KEY_STORE.setCertificateEntry("extra_ssl_" + i, a.get(i));
            }
            tmf.init(KEY_STORE);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            SSLContext.setDefault(sslContext);
            return sslContext;
        } catch (Exception e) {
            return null;
        }
    }
}