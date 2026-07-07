package com.roomfit.room;

import com.roomfit.common.CommonResponse;
import com.roomfit.room.dto.FurnitureUpdateRequest;
import com.roomfit.room.dto.RoomResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/{roomId}")
    public CommonResponse<RoomResponse> getRoom(@PathVariable Long roomId) {
        return CommonResponse.ok(roomService.getRoom(roomId));
    }

    @PutMapping("/{roomId}/furniture")
    public CommonResponse<RoomResponse> updateFurniture(@PathVariable Long roomId,
                                                          @RequestBody FurnitureUpdateRequest request) {
        return CommonResponse.ok(roomService.updateFurnitureStatus(roomId, request));
    }

    // TODO: GET /api/rooms/samples (선택 구현) - 샘플 방 목록 요약 조회
}
