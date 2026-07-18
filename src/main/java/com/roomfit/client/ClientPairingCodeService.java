package com.roomfit.client;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;

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
     *
     * "조회 후 없으면 생성"은 원자적이지 않다 — 같은 clientId로 거의 동시에 두
     * 요청이 들어오면(화면이 재진입하며 fetch가 겹치는 경우 등) 둘 다 조회에서
     * "없음"을 보고 둘 다 insert를 시도할 수 있다(재현 확인함: 동시 요청 2개 중
     * 하나가 client_id unique 제약 위반으로 500). 이 메서드에 @Transactional을
     * 걸지 않아 findByClientId/save가 각각 독립된 트랜잭션으로 실행되게 한다 —
     * 그래야 save 실패가 그 트랜잭션만 롤백시키고, catch 블록의 재조회는 새
     * 트랜잭션에서 깨끗하게 실행된다(같은 트랜잭션 안에서 잡으면 Hibernate
     * 세션이 이미 무효화된 상태라 재조회도 같이 실패한다). 제약 위반이 나면
     * 먼저 이긴 요청이 저장한 코드를 그대로 재사용한다.
     */
    public String issueOrGetCode() {
        String clientId = requireClientId();
        Optional<String> existing = findCodeByClientId(clientId);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return saveNewCode(clientId);
        } catch (DataIntegrityViolationException e) {
            return findCodeByClientId(clientId).orElseThrow(() -> e);
        }
    }

    /**
     * 기존 코드를 무효화하고 새 코드를 발급한다 — 예전 코드는 그 즉시 redeem이
     * 안 된다(같은 row의 code 컬럼을 덮어쓰므로). issueOrGetCode와 같은 이유로,
     * 아직 코드가 없던 clientId에 재발급이 동시에 두 번 들어오는 드문 경우까지
     * insert 경쟁 상태를 방어한다.
     */
    public String regenerateCode() {
        String clientId = requireClientId();
        Optional<ClientPairingCode> existing = findByClientId(clientId);
        if (existing.isPresent()) {
            return rotateAndSave(existing.get());
        }

        try {
            return saveNewCode(clientId);
        } catch (DataIntegrityViolationException e) {
            ClientPairingCode pairingCode = findByClientId(clientId).orElseThrow(() -> e);
            return rotateAndSave(pairingCode);
        }
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

    // 이 세 헬퍼는 일부러 @Transactional을 붙이지 않는다 — 같은 클래스 안에서
    // this.xxx()로 호출되는 메서드에 붙이면 Spring AOP 프록시를 우회해 실제로는
    // 아무 효과가 없다(자기 자신 호출은 프록시를 안 거치므로). 대신
    // repository.save/findBy... 각각이 Spring Data JPA 자체적으로 이미 독립된
    // 트랜잭션으로 실행되므로, 이 메서드들은 그 호출을 감싸는 순수 헬퍼일 뿐이다.
    private Optional<ClientPairingCode> findByClientId(String clientId) {
        return repository.findByClientId(clientId);
    }

    private Optional<String> findCodeByClientId(String clientId) {
        return findByClientId(clientId).map(ClientPairingCode::getCode);
    }

    private String saveNewCode(String clientId) {
        return repository.save(new ClientPairingCode(clientId, generateUniqueCode())).getCode();
    }

    private String rotateAndSave(ClientPairingCode pairingCode) {
        pairingCode.rotateCode(generateUniqueCode());
        return repository.save(pairingCode).getCode();
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
