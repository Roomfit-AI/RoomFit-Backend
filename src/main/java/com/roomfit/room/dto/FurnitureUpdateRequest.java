package com.roomfit.room.dto;

import java.util.List;

public class FurnitureUpdateRequest {

    private List<Item> furnitureUpdates;

    protected FurnitureUpdateRequest() {
        // JSON 역직렬화용
    }

    public List<Item> getFurnitureUpdates() {
        return furnitureUpdates;
    }

    public static class Item {
        private String id;
        private String status; // "existing" or "deleted"

        protected Item() {
        }

        public String getId() {
            return id;
        }

        public String getStatus() {
            return status;
        }
    }
}
