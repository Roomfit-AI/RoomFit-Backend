package com.roomfit.room;

import com.roomfit.common.CommonResponse;
import com.roomfit.room.dto.FurnitureUpdateRequest;
import com.roomfit.room.dto.RoomResponse;
import com.roomfit.room.dto.RoomUploadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "방 조회, RoomPlan 업로드, 기존 가구 상태 수정 API")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/samples")
    @Operation(summary = "샘플 방 목록 조회", description = "데모 시작 시 사용할 샘플 방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "샘플 방 목록 조회 성공")
    public CommonResponse<List<RoomResponse>> getSampleRooms() {
        return CommonResponse.ok(roomService.getSampleRooms());
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "특정 방 조회", description = "roomId 기준으로 방 크기, 문/창문, 기존 가구 목록을 조회합니다. 프론트는 이 응답을 기준으로 방을 렌더링합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "방 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 roomId")
    })
    public CommonResponse<RoomResponse> getRoom(@PathVariable Long roomId) {
        return CommonResponse.ok(roomService.getRoom(roomId));
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "RoomPlan JSON 업로드", description = "iOS RoomPlan App 또는 Mock/Manual 입력에서 생성한 RoomFit JSON을 백엔드에 저장하고 roomId를 발급합니다. 업로드된 방은 GET /api/rooms/{roomId}로 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "RoomFit JSON 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 방 크기, 문/창문 데이터, 가구 위치 또는 요청 본문")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "RoomPlan 또는 Mock/Manual 입력에서 생성한 RoomFit JSON",
            required = true,
            content = @Content(examples = @ExampleObject(name = "Mock Room upload", value = """
                    {
                      "name": "RoomPlan Scan Room",
                      "room": {
                        "width": 3.2,
                        "depth": 4.5,
                        "height": 2.4,
                        "unit": "meter"
                      },
                      "openings": [],
                      "furniture": []
                    }
                    """)))
    public CommonResponse<RoomResponse> uploadRoom(@RequestBody RoomUploadRequest request) {
        return CommonResponse.ok(roomService.uploadRoom(request));
    }

    @PutMapping("/{roomId}/furniture")
    @Operation(summary = "기존 가구 상태/위치 수정", description = "기존 가구를 삭제하거나 위치/회전/상태를 수정합니다. 예를 들어 기존 책상을 DELETED로 변경한 뒤 새 책상을 추천받을 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "기존 가구 상태 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 가구 status"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 roomId 또는 furniture id")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "기존 가구 상태 변경 요청",
            required = true,
            content = @Content(examples = @ExampleObject(name = "Delete existing desk", value = """
                    {
                      "furnitureUpdates": [
                        { "id": "desk-1", "status": "DELETED" }
                      ]
                    }
                    """)))
    public CommonResponse<RoomResponse> updateFurniture(@PathVariable Long roomId,
                                                          @RequestBody FurnitureUpdateRequest request) {
        return CommonResponse.ok(roomService.updateFurnitureStatus(roomId, request));
    }
}
