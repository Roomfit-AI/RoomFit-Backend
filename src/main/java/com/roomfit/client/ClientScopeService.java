package com.roomfit.client;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ClientScopeService {

    public static final String HEADER_NAME = "X-RoomFit-Client-Id";
    private static final int MAX_CLIENT_ID_LENGTH = 64;
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final ClientScopeProperties properties;

    public ClientScopeService(ClientScopeProperties properties) {
        this.properties = properties;
    }

    public ClientScope resolve(String rawClientId) {
        if (!properties.isEnabled()) {
            return ClientScope.disabled();
        }
        if (rawClientId == null || rawClientId.isBlank()) {
            if (properties.isRequired()) {
                throw new CustomException(ErrorCode.CLIENT_ID_REQUIRED);
            }
            return ClientScope.legacyScope();
        }

        String clientId = rawClientId.trim();
        if (clientId.length() > MAX_CLIENT_ID_LENGTH || !UUID_PATTERN.matcher(clientId).matches()) {
            throw new CustomException(ErrorCode.INVALID_CLIENT_ID);
        }
        try {
            return ClientScope.client(UUID.fromString(clientId).toString().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CLIENT_ID);
        }
    }
}
