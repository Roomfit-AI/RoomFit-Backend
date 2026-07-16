package com.roomfit.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "manage-furniture 단계(아직 AI 추천/Layout이 생성되기 전)의 가구 추가/이동/삭제/회전을 " +
        "한 번에 반영하는 전체 배열 교체 요청. rooms furniture status 변경 API(PUT /{roomId}/furniture)와" +
        " 달리 새 가구 추가와 위치/회전 변경까지 포함한 전체 목록을 그대로 전달한다.")
public class RoomFurnitureReplaceRequest {

    @Schema(description = "이 방의 최종 가구 전체 목록. 기존 RoomUploadRequest의 furniture와 같은 shape.")
    private List<RoomUploadRequest.FurnitureData> furniture;

    protected RoomFurnitureReplaceRequest() {
        // JSON 역직렬화용
    }

    public List<RoomUploadRequest.FurnitureData> getFurniture() {
        return furniture;
    }
}
