package io.github.areiljan.cmrsign.cades;

import io.github.areiljan.cmrsign.Indication;
import io.github.areiljan.cmrsign.PadesMocksKt;
import io.github.areiljan.cmrsign.Qualification;
import io.github.areiljan.cmrsign.SignerIdentity;
import io.github.areiljan.cmrsign.SigningProvider;
import io.github.areiljan.cmrsign.Verdict;
import io.github.areiljan.cmrsign.efti.EftiDataElement;
import io.github.areiljan.cmrsign.efti.EftiSignatureStatus;
import io.github.areiljan.cmrsign.pades.PadesSigner;
import io.github.areiljan.cmrsign.pades.PadesValidator;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaApiTest {
    private final KeyPair keyPair = PadesMocksKt.keyPair();
    private final X509Certificate certificate = PadesMocksKt.certificate(keyPair);

    @Test
    void signsValidatesAndDerivesTheEftiStatusFromJava() {
        var provider = new KeyPairProvider();
        var session = provider.startSession("Java signer");
        var identity = provider.completeSession(session);

        var signed = PadesSigner.forProvider(provider, session, identity).sign(PadesMocksKt.unsignedPdf(), "Java signer");
        var verdict = new PadesValidator(new DssCmsValidator(List.of(certificate))).validate(signed);
        var efti = EftiSignatureStatus.from(EftiDataElement.EFTI65_SENDER, verdict, "report-1");

        assertEquals(Indication.TOTAL_PASSED, verdict.getIndication());
        assertFalse(efti.getAtLeastAdvanced());
    }

    @Test
    void buildsVerdictsAndIdentitiesWithoutKotlinDefaults() {
        var verdict = Verdict.from(Indication.TOTAL_PASSED, null, Qualification.NA, new SignerIdentity("CN=Java"), "report");

        assertEquals("CN=Java", verdict.getSigner().getSubjectName());
        assertTrue(verdict.getSigner().getCertificateChain().isEmpty());
    }

    private class KeyPairProvider implements SigningProvider {
        @Override
        public String startSession(String signer) {
            return "session";
        }

        @Override
        public SignerIdentity completeSession(String session) {
            return new SignerIdentity(certificate.getSubjectX500Principal().getName(), List.of(certificate));
        }

        @Override
        public byte[] sign(String session, byte[] dataToSign) {
            try {
                var signature = Signature.getInstance("SHA256withRSA");
                signature.initSign(keyPair.getPrivate());
                signature.update(dataToSign);
                return signature.sign();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
