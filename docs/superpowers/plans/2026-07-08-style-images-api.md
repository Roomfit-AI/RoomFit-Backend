# Style Images API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `GET /api/styles/images` API를 추가해 RoomFit AI MVP에서 선택 가능한 취향 이미지 목록을 `CommonResponse` 구조로 반환한다.

**Architecture:** `com.roomfit.style` 도메인을 새로 만들고, 참고 저장소 구조처럼 `controller`, `domain`, `dto/response`, `repository`, `service` 하위 패키지로 역할을 나눈다. Repository는 읽기 전용 in-memory list를 보관하고, Service는 domain을 response DTO로 변환하며, Controller는 `CommonResponse.ok(data)`로 감싸 반환한다.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring MVC, JUnit 5, Spring Boot Test, MockMvc.

## Global Constraints

- 최종 RoomFit AI 백엔드 API 명세서 v1.0을 endpoint, 응답 구조, 필드명, 데이터 예시의 최우선 기준으로 삼는다.
- API 응답 body는 `success`, `data`, `error`만 포함하는 `CommonResponse<T>` 구조를 따른다.
- `GET /api/styles/images`만 구현하고, Agent Context 생성, `selectedImageIds` 검증, `styleTags` 통합 저장, 추천, 검증, 수정, 피드백, 확정 기능은 구현하지 않는다.
- 명세서 예시의 취향 이미지 3개만 반환한다.
- 명세서에 없는 응답 필드, enum 값, ErrorCode를 추가하지 않는다.
- 인증/인가는 MVP 범위에서 생략한다.
- 생성하는 style 관련 파일은 `com.roomfit.style` 아래에 둔다.

---

## 파일 구조

새로 생성할 파일:

- `src/main/java/com/roomfit/style/controller/StyleImageController.java`
  - `GET /api/styles/images` endpoint를 제공한다.
- `src/main/java/com/roomfit/style/domain/StyleImage.java`
  - Style Image domain 모델이다.
- `src/main/java/com/roomfit/style/dto/response/StyleImageResponse.java`
  - API 응답 DTO이다.
- `src/main/java/com/roomfit/style/repository/StyleImageRepository.java`
  - 읽기 전용 in-memory Style Image 목록을 제공한다.
- `src/main/java/com/roomfit/style/service/StyleImageService.java`
  - Repository 조회 결과를 response DTO로 변환한다.
- `src/test/java/com/roomfit/style/controller/StyleImageControllerTest.java`
  - `GET /api/styles/images` MVC 테스트이다.

수정할 파일:

- 없음.

---

### Task 1: Style Image 목록 API

**Files:**
- Create: `src/test/java/com/roomfit/style/controller/StyleImageControllerTest.java`
- Create: `src/main/java/com/roomfit/style/controller/StyleImageController.java`
- Create: `src/main/java/com/roomfit/style/domain/StyleImage.java`
- Create: `src/main/java/com/roomfit/style/dto/response/StyleImageResponse.java`
- Create: `src/main/java/com/roomfit/style/repository/StyleImageRepository.java`
- Create: `src/main/java/com/roomfit/style/service/StyleImageService.java`

**Interfaces:**
- Consumes:
  - `com.roomfit.common.CommonResponse.ok(T data)`
- Produces:
  - `StyleImageController.getStyleImages(): CommonResponse<List<StyleImageResponse>>`
  - `StyleImageService.getStyleImages(): List<StyleImageResponse>`
  - `StyleImageRepository.findAll(): List<StyleImage>`
  - `StyleImageResponse.from(StyleImage styleImage): StyleImageResponse`

- [ ] **Step 1: 실패하는 MVC 테스트 작성**

`src/test/java/com/roomfit/style/controller/StyleImageControllerTest.java`를 생성한다.

```java
package com.roomfit.style.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StyleImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getStyleImages_returnsSpecStyleImageExamples() throws Exception {
        mockMvc.perform(get("/api/styles/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[*].imageId").value(containsInAnyOrder(1, 2, 3)))
                .andExpect(jsonPath("$.data[*].title").value(hasItems(
                        "화이트톤 미니멀 원룸",
                        "내추럴 우드톤 원룸",
                        "공부형 원룸 인테리어"
                )))
                .andExpect(jsonPath("$.data[*].imageUrl", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].tags", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[0].tags").value(hasItems(
                        "minimal", "white_tone", "open_space"
                )))
                .andExpect(jsonPath("$.data[1].tags").value(hasItems(
                        "natural", "wood_tone", "cozy"
                )))
                .andExpect(jsonPath("$.data[2].tags").value(hasItems(
                        "study", "desk_zone", "minimal"
                )));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run:

```bash
./gradlew test --tests com.roomfit.style.controller.StyleImageControllerTest
```

Expected:

```text
BUILD FAILED
```

예상 실패 이유는 `404 Not Found` 또는 상태코드 assertion 실패이다. 아직 `GET /api/styles/images` controller가 없기 때문에 실패해야 한다.

- [ ] **Step 3: domain 모델 생성**

`src/main/java/com/roomfit/style/domain/StyleImage.java`를 생성한다.

```java
package com.roomfit.style.domain;

import java.util.List;

public class StyleImage {

    private final Long imageId;
    private final String title;
    private final String imageUrl;
    private final List<String> tags;

    public StyleImage(Long imageId, String title, String imageUrl, List<String> tags) {
        this.imageId = imageId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.tags = List.copyOf(tags);
    }

    public Long getImageId() {
        return imageId;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getTags() {
        return tags;
    }
}
```

- [ ] **Step 4: response DTO 생성**

`src/main/java/com/roomfit/style/dto/response/StyleImageResponse.java`를 생성한다.

```java
package com.roomfit.style.dto.response;

import com.roomfit.style.domain.StyleImage;

import java.util.List;

public class StyleImageResponse {

    private final Long imageId;
    private final String title;
    private final String imageUrl;
    private final List<String> tags;

    private StyleImageResponse(Long imageId, String title, String imageUrl, List<String> tags) {
        this.imageId = imageId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.tags = tags;
    }

    public static StyleImageResponse from(StyleImage styleImage) {
        return new StyleImageResponse(
                styleImage.getImageId(),
                styleImage.getTitle(),
                styleImage.getImageUrl(),
                styleImage.getTags()
        );
    }

    public Long getImageId() {
        return imageId;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getTags() {
        return tags;
    }
}
```

- [ ] **Step 5: Repository 생성**

`src/main/java/com/roomfit/style/repository/StyleImageRepository.java`를 생성한다.

```java
package com.roomfit.style.repository;

import com.roomfit.style.domain.StyleImage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StyleImageRepository {

    private final List<StyleImage> styleImages = List.of(
            new StyleImage(
                    1L,
                    "화이트톤 미니멀 원룸",
                    "/images/styles/minimal-white-1.jpg",
                    List.of("minimal", "white_tone", "open_space")
            ),
            new StyleImage(
                    2L,
                    "내추럴 우드톤 원룸",
                    "/images/styles/natural-wood-1.jpg",
                    List.of("natural", "wood_tone", "cozy")
            ),
            new StyleImage(
                    3L,
                    "공부형 원룸 인테리어",
                    "/images/styles/study-focused-1.jpg",
                    List.of("study", "desk_zone", "minimal")
            )
    );

    public List<StyleImage> findAll() {
        return styleImages;
    }
}
```

- [ ] **Step 6: Service 생성**

`src/main/java/com/roomfit/style/service/StyleImageService.java`를 생성한다.

```java
package com.roomfit.style.service;

import com.roomfit.style.dto.response.StyleImageResponse;
import com.roomfit.style.repository.StyleImageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StyleImageService {

    private final StyleImageRepository styleImageRepository;

    public StyleImageService(StyleImageRepository styleImageRepository) {
        this.styleImageRepository = styleImageRepository;
    }

    public List<StyleImageResponse> getStyleImages() {
        return styleImageRepository.findAll().stream()
                .map(StyleImageResponse::from)
                .toList();
    }
}
```

- [ ] **Step 7: Controller 생성**

`src/main/java/com/roomfit/style/controller/StyleImageController.java`를 생성한다.

```java
package com.roomfit.style.controller;

import com.roomfit.common.CommonResponse;
import com.roomfit.style.dto.response.StyleImageResponse;
import com.roomfit.style.service.StyleImageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/styles")
public class StyleImageController {

    private final StyleImageService styleImageService;

    public StyleImageController(StyleImageService styleImageService) {
        this.styleImageService = styleImageService;
    }

    @GetMapping("/images")
    public CommonResponse<List<StyleImageResponse>> getStyleImages() {
        return CommonResponse.ok(styleImageService.getStyleImages());
    }
}
```

- [ ] **Step 8: 대상 테스트 통과 확인**

Run:

```bash
./gradlew test --tests com.roomfit.style.controller.StyleImageControllerTest
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 9: 전체 테스트 통과 확인**

Run:

```bash
./gradlew test
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 10: 변경사항 검토**

Run:

```bash
git diff -- src/main/java/com/roomfit/style src/test/java/com/roomfit/style
```

검토 기준:

- `GET /api/styles/images` 외 endpoint가 추가되지 않았다.
- 응답 필드는 명세서 필드인 `imageId`, `title`, `imageUrl`, `tags`만 포함한다.
- `CommonResponse.ok(data)`를 사용한다.
- `ErrorCode`를 수정하지 않았다.
- `room`, `agent`, `placement`, `product` 패키지를 수정하지 않았다.
- 테스트가 명세 예시 3개 이미지와 태그를 확인한다.

- [ ] **Step 11: 커밋**

Run:

```bash
git add src/main/java/com/roomfit/style src/test/java/com/roomfit/style
git commit -m "feat: add style images api"
```

Expected:

```text
[develop <commit>] feat: add style images api
```

---

## 계획 자체 검토

- Spec coverage: `GET /api/styles/images`, 3개 명세 예시 데이터, `CommonResponse`, 명세 필드, 참고 저장소형 패키지 구조, 테스트 요구사항을 Task 1에 반영했다.
- Placeholder scan: 빈칸으로 남긴 구현 지시나 모호한 후속 작업 표현을 남기지 않았다.
- Type consistency: `StyleImage`, `StyleImageResponse`, `StyleImageRepository`, `StyleImageService`, `StyleImageController`의 메서드명과 반환 타입은 각 단계에서 일관되게 사용한다.
