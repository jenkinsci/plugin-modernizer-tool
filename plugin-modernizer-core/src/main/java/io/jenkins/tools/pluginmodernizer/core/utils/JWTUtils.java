package io.jenkins.tools.pluginmodernizer.core.utils;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jsonwebtoken.Jwts;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Date;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/**
 * Utility class for JWT operations.
 */
public final class JWTUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Return the private key in RSA format from a PEM file PKCS#8 keys (downloaded from GitHub)
     * @param pemFile the path to the PEM file
     * @return the private key
     */
    private static PrivateKey buildPrivateKey(Path pemFile) {
        try (PEMParser pemParser = new PEMParser(new FileReader(pemFile.toFile(), StandardCharsets.UTF_8))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            if (object instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else {
                throw new ModernizerException("Invalid PEM file format");
            }
        } catch (Exception e) {
            throw new ModernizerException("Error reading PEM file", e);
        }
    }

    /**
     * Get JWT token
     * @param config The configuration
     * @return the JWT token
     */
    public static String getJWT(Config config, Path pemFile) {
        return Jwts.builder()
                .issuedAt(new Date(System.currentTimeMillis()))
                .issuer(config.getGithubAppId().toString())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(buildPrivateKey(pemFile), Jwts.SIG.RS256)
                .compact();
    }
}
