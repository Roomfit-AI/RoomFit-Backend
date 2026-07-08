package com.roomfit.room;

import com.roomfit.common.CommonResponse;
import com.roomfit.room.dto.FurnitureUpdateRequest;
import com.roomfit.room.dto.RoomResponse;
import com.roomfit.room.dto.RoomUploadRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/samples")
    public CommonResponse<List<RoomResponse>> getSampleRooms() {
        return CommonResponse.ok(roomService.getSampleRooms());
    }

    @GetMapping("/{roomId}")
    public CommonResponse<RoomResponse> getRoom(@PathVariable Long roomId) {
        return CommonResponse.ok(roomService.getRoom(roomId));
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<RoomResponse> uploadRoom(@RequestBody RoomUploadRequest request) {
        return CommonResponse.ok(roomService.uploadRoom(request));
    }

    @PutMapping("/{roomId}/furniture")
    public CommonResponse<RoomResponse> updateFurniture(@PathVariable Long roomId,
                                                          @RequestBody FurnitureUpdateRequest request) {
        return CommonResponse.ok(roomService.updateFurnitureStatus(roomId, request));
    }
}
