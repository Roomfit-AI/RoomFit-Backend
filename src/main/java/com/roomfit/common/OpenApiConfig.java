package com.roomfit.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI roomFitOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RoomFit-Backend")
                        .version("0.0.1")
                        .description("""
                                RoomFit AI MVP backend API.

                                RoomFit MVP Demo Flow:
                                1. GET /api/rooms/1
                                2. GET /api/products/mock
                                3. PUT /api/rooms/1/furniture
                                4. POST /api/agent/context
                                5. POST /api/layouts/recommend
                                6. POST /api/layouts/validate
                                7. PUT /api/layouts/{layoutId}

                                Notes for frontend integration:
                                - 단위는 meter입니다.
                                - 좌표계는 x-z 평면입니다.
                                - position은 가구 중심 좌표입니다.
                                - rotation은 degree입니다.
                                - DELETED 가구는 렌더링/검증에서 제외하는 것이 자연스럽습니다.
                                - POST /api/layouts/validate는 저장하지 않습니다.
                                - PUT /api/layouts/{layoutId}는 저장 + 재검증 + 점수 재계산을 수행합니다.
                                - 현재 MVP는 in-memory repository를 사용하므로 서버 재시작 시 업로드 room/context/layout 데이터가 초기화됩니다.
                                """))
                .addTagsItem(new Tag().name("Rooms").description("방 조회, RoomPlan 업로드, 기존 가구 상태 수정 API"))
                .addTagsItem(new Tag().name("Products").description("MVP 제품 카드 렌더링용 Mock Product API"))
                .addTagsItem(new Tag().name("Agent").description("사용자 목표/스타일/선택 제품 기반 추천 Context 생성 API"))
                .addTagsItem(new Tag().name("Layouts").description("추천 배치 생성, 드래그 검증, 수정 저장, 피드백, 확정 API"))
                .addTagsItem(new Tag().name("Styles").description("스타일 이미지 선택용 API"));
    }
}
