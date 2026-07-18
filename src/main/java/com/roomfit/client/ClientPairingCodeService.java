package com.roomfit.client;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;

@Service
public class ClientPairingCodeService {

    private static final int CODE_LENGTH = 8;
    // 사람이 눈으로 읽고 손으로 타이핑할 코드라 0/O, 1/I/L처럼 헷갈리는 문자를 뺐다.
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClientPairingCodeRepository repository;
    private final ClientScopeContext clientScopeContext;

    public ClientPairingCodeService(ClientPairingCodeRepository repository, ClientScopeContext clientScopeContext) {
        this.repository = repository;
        this.clientScopeContext = clientScopeContext;
    }

    /**
     * 이미 이 clientId로 발급된 코드가 있으면 그대로 반환한다(멱등) — 앱이 매번
     * 새로 만들지 않고 같은 코드를 계속 보여줄 수 있게.
     */
    @Transactional
    public String issueOrGetCode() {
        String clientId = requireClientId();
        return repository.findByClientId(clientId)
                .map(ClientPairingCode::getCode)
                .orElseGet(() -> repository.save(new ClientPairingCode(clientId, generateUniqueCode())).getCode());
    }

    /**
     * 기존 코드를 무효화하고 새 코드를 발급한다 — 예전 코드는 그 즉시 redeem이
     * 안 된다(같은 row의 code 컬럼을 덮어쓰므로).
     */
    @Transactional
    public String regenerateCode() {
        String clientId = requireClientId();
        ClientPairingCode pairingCode = repository.findByClientId(clientId)
                .orElseGet(() -> new ClientPairingCode(clientId, generateUniqueCode()));
        pairingCode.rotateCode(generateUniqueCode());
        return repository.save(pairingCode).getCode();
    }

    /**
     * 코드로 clientId를 찾는다 — 이 호출 자체는 인증이 필요 없다(아직 신원을
     * 모르는 브라우저가 자기 clientId를 알아내려고 부르는 호출이라서).
     */
    @Transactional(readOnly = true)
    public String redeem(String rawCode) {
        String normalized = normalize(rawCode);
        return repository.findByCode(normalized)
                .map(ClientPairingCode::getClientId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAIRING_CODE_NOT_FOUND));
    }

    private String requireClientId() {
        ClientScope scope = clientScopeContext.current();
        if (!scope.enabled() || scope.legacy()) {
            throw new CustomException(ErrorCode.CLIENT_ID_REQUIRED);
        }
        return scope.id();
    }

    private String normalize(String rawCode) {
        if (rawCode == null) {
            throw new CustomException(ErrorCode.PAIRING_CODE_NOT_FOUND);
        }
        // 화면에 "K7X9-QP42"처럼 대시를 넣어 보여줄 걸 감안해 대시/공백은 무시하고
        // 대소문자도 구분하지 않는다.
        String cleaned = rawCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (cleaned.length() != CODE_LENGTH) {
            throw new CustomException(ErrorCode.PAIRING_CODE_NOT_FOUND);
        }
        return cleaned;
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = generateCode();
        } while (repository.existsByCode(code));
        return code;
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }
}
