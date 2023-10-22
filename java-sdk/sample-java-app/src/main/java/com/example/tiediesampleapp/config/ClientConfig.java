// Copyright (c) 2023, Cisco and/or its affiliates.
// All rights reserved.
// See license in distribution for details.

package com.example.tiediesampleapp.config;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Optional;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

public abstract class ClientConfig {
    protected static final String CA_PEM_PATH = "/ca.pem";

    protected String getCnFromKeyStore(KeyStore keyStore) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            String alias = aliases.nextElement();

            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

            return getCnFromCert(certificate);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getCnFromCert(X509Certificate certificate) {
        try {
            X500Principal principal = certificate.getSubjectX500Principal();

            LdapName ldapName = new LdapName(principal.getName());

            Optional<Rdn> rdn = ldapName.getRdns().stream().filter(i -> i.getType().equalsIgnoreCase("CN"))
                    .findFirst();

            if (rdn.isEmpty()) {
                throw new RuntimeException("No CN found in certificate");
            }

            return rdn.get().getValue().toString();
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }
    }
}
