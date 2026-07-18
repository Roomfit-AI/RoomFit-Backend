package com.roomfit.client;

/**
 * Request-local anonymous client scope. The legacy scope deliberately keeps
 * header-less clients working while the web and RoomPlan clients migrate.
 */
public record ClientScope(boolean enabled, String id, boolean legacy) {

    public static final String LEGACY_ID = "legacy";

    public static ClientScope disabled() {
        return new ClientScope(false, null, true);
    }

    public static ClientScope legacyScope() {
        return new ClientScope(true, LEGACY_ID, true);
    }

    public static ClientScope client(String id) {
        return new ClientScope(true, id, false);
    }
}
