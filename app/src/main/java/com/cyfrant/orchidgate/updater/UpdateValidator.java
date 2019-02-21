package com.cyfrant.orchidgate.updater;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Base64;

import com.cyfrant.orchidgate.contract.Proxy;
import com.google.common.io.ByteStreams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class UpdateValidator {
    public static final String CERT_TYPE = "X.509";
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final String CERT_ASSET = "vendor.cer";
    public static final String ALGO_SIGNATURE = "SHA256withRSA";

    public static class SignedData {
        private final byte[] data;
        private final byte[] signature;

        private SignedData(String data, String signature) throws UnsupportedEncodingException {
            this.data = data.getBytes(CHARSET_UTF8);
            this.signature = Base64.decode(signature, Base64.DEFAULT);
        }
    }

    private final Update reference;
    private final File apk;
    private final Application application;

    public UpdateValidator(Application self, Update update, File installationPackage) {
        apk = installationPackage;
        reference = update;
        application = self;
    }

    public String validateUpdate(Proxy proxy) {
        String message = "";
        try {
            if (verifyUpdateSignature(reference, application)) {
                download(reference.getLocation(), apk, proxy);
                if (verifyPackageSignature(apk, reference, application)) {
                    PackageInfo info = application.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
                    if (info != null) {
                        String packageName = info.packageName;
                        long versionCode = info.getLongVersionCode();
                        if (application.getPackageName().equalsIgnoreCase(packageName)) {
                            long appVersion = application.getPackageManager().getPackageInfo(application.getPackageName(), 0).getLongVersionCode();
                            if (appVersion < versionCode) {

                            } else {
                                message = "Update version " + versionCode + " elder or the same as installed: " + appVersion;
                            }
                        } else {
                            message = "Downloaded package " + packageName + " mismatched intended package " + application.getPackageName();
                        }
                    } else {
                        message = "Couldn't obtain downloaded package info";
                    }
                } else {
                    message = "Update package " + apk.getAbsolutePath() + " abandoned or tampered";
                }
            } else {
                message = "Update entry (ver." + reference.getVersion() + ":"
                        + reference.getLocation() + ") signature corrupted";
            }
        } catch (IOException | CertificateException | NoSuchAlgorithmException | InvalidKeyException
                | SignatureException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            message = e.getMessage();
        }
        return message;
    }

    public static String get(String url, Proxy proxyService) {
        HttpURLConnection connection = null;
        try {
            java.net.Proxy proxy = (proxyService == null || proxyService.getProxyPort() == -1) ?
                    java.net.Proxy.NO_PROXY :
                    new java.net.Proxy(
                            java.net.Proxy.Type.SOCKS,
                            new InetSocketAddress("127.0.0.1", proxyService.getProxyPort())
                    );
            connection = (HttpURLConnection) new URL(url).openConnection(proxy);
            connection.setDoOutput(false);
            connection.setChunkedStreamingMode(0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteStreams.copy(connection.getInputStream(), out);
            return new String(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {

        }
    }

    public static void download(String url, File target, Proxy proxyService) throws IOException {
        HttpURLConnection connection = null;
        FileOutputStream out = null;
        try {
            java.net.Proxy proxy = (proxyService == null || proxyService.getProxyPort() == -1) ?
                    java.net.Proxy.NO_PROXY :
                    new java.net.Proxy(
                            java.net.Proxy.Type.SOCKS,
                            new InetSocketAddress("127.0.0.1", proxyService.getProxyPort())
                    );
            connection = (HttpURLConnection) new URL(url).openConnection(proxy);
            connection.setDoOutput(false);
            connection.setChunkedStreamingMode(0);
            out = new FileOutputStream(target);
            ByteStreams.copy(connection.getInputStream(), out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static X509Certificate getVendorCertificate(Application application) throws CertificateException, IOException {
        InputStream in = null;
        try {
            in = application.getAssets().open(CERT_ASSET);
            return (X509Certificate) CertificateFactory.getInstance(CERT_TYPE)
                    .generateCertificate(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static Signature getVerifier(Application application) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        X509Certificate trust = getVendorCertificate(application);
        Signature signature = Signature.getInstance(ALGO_SIGNATURE);
        signature.initVerify(trust);
        return signature;
    }

    public static boolean verifyUpdateSignature(Update update, Application application) throws IOException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        String data = update.getVersion()
                + "|" + update.getLocation()
                + "|" + update.getPackageSignature();
        SignedData signedData = new SignedData(data, update.getUpdateSignature());
        Signature signature = getVerifier(application);
        signature.update(signedData.data);
        return signature.verify(signedData.signature);
    }

    public static boolean verifyPackageSignature(File file, Update update, Application application) throws IOException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        InputStream in = null;
        int count = -1;
        byte[] buffer = new byte[4096];
        try {
            in = new FileInputStream(file);
            Signature verifier = getVerifier(application);
            do {
                count = in.read(buffer);
                if (count > 0) {
                    verifier.update(buffer, 0, count);
                }
            } while (count > 0);
            byte[] signature = Base64.decode(update.getPackageSignature(), Base64.DEFAULT);
            return verifier.verify(signature);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
