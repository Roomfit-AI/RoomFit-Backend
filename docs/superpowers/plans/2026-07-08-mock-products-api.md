# Mock Products API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `GET /api/products/mock` API를 추가해 RoomFit AI MVP에서 선택 가능한 Mock Product 목록을 `CommonResponse` 구조로 반환한다.

**Architecture:** `com.roomfit.product` 도메인을 새로 만들고, 참고 저장소 구조처럼 `controller`, `domain`, `dto/response`, `repository`, `service` 하위 패키지로 역할을 나눈다. Repository는 읽기 전용 in-memory list를 보관하고, Service는 domain을 response DTO로 변환하며, Controller는 `CommonResponse.ok(data)`로 감싸 반환한다.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring MVC, JUnit 5, Spring Boot Test, MockMvc, AssertJ.

## Global Constraints

- 최종 RoomFit AI 백엔드 API 명세서 v1.0을 endpoint, 응답 구조, 필드명, enum/허용값의 최우선 기준으로 삼는다.
- API 응답 body는 `success`, `data`, `error`만 포함하는 `CommonResponse<T>` 구조를 따른다.
- `GET /api/products/mock`만 구현하고, 취향 이미지 목록, Agent Context, 추천, 검증, 수정, 피드백, 확정 기능은 구현하지 않는다.
- `bed`, `desk`, `chair`, `storage`, `rug`, `lamp` 6개 타입을 모두 Mock Product 데이터에 포함한다.
- 명세서 예시 product id인 `desk-01`, `chair-01`, `lamp-01`은 유지한다.
- 명세서에 없는 응답 필드, enum 값, ErrorCode를 추가하지 않는다.
- 인증/인가는 MVP 범위에서 생략한다.
- 실제 쇼핑몰 제품 추천은 구현하지 않고, Mock Product 데이터만 사용한다.
- 생성하는 product 관련 파일은 `com.roomfit.product` 아래에 둔다.

---

## 파일 구조

새로 생성할 파일:

- `src/main/java/com/roomfit/product/controller/MockProductController.java`
  - `GET /api/products/mock` endpoint를 제공한다.
- `src/main/java/com/roomfit/product/domain/MockProduct.java`
  - Mock Product domain 모델이다.
- `src/main/java/com/roomfit/product/domain/RequiredClearance.java`
  - `requiredClearance.front`, `requiredClearance.side` 값을 표현한다.
- `src/main/java/com/roomfit/product/dto/response/MockProductResponse.java`
  - API 응답 DTO이다.
- `src/main/java/com/roomfit/product/dto/response/RequiredClearanceResponse.java`
  - nested `requiredClearance` 응답 DTO이다.
- `src/main/java/com/roomfit/product/repository/MockProductRepository.java`
  - 읽기 전용 in-memory Mock Product 목록을 제공한다.
- `src/main/java/com/roomfit/product/service/MockProductService.java`
  - Repository 조회 결과를 response DTO로 변환한다.
- `src/test/java/com/roomfit/product/controller/MockProductControllerTest.java`
  - `GET /api/products/mock` MVC 테스트이다.

수정할 파일:

- 없음.

---

### Task 1: Mock Product 목록 API

**Files:**
- Create: `src/test/java/com/roomfit/product/controller/MockProductControllerTest.java`
- Create: `src/main/java/com/roomfit/product/controller/MockProductController.java`
- Create: `src/main/java/com/roomfit/product/domain/MockProduct.java`
- Create: `src/main/java/com/roomfit/product/domain/RequiredClearance.java`
- Create: `src/main/java/com/roomfit/product/dto/response/MockProductResponse.java`
- Create: `src/main/java/com/roomfit/product/dto/response/RequiredClearanceResponse.java`
- Create: `src/main/java/com/roomfit/product/repository/MockProductRepository.java`
- Create: `src/main/java/com/roomfit/product/service/MockProductService.java`

**Interfaces:**
- Consumes:
  - `com.roomfit.common.CommonResponse.ok(T data)`
- Produces:
  - `MockProductController.getMockProducts(): CommonResponse<List<MockProductResponse>>`
  - `MockProductService.getMockProducts(): List<MockProductResponse>`
  - `MockProductRepository.findAll(): List<MockProduct>`
  - `MockProductResponse.from(MockProduct product): MockProductResponse`
  - `RequiredClearanceResponse.from(RequiredClearance requiredClearance): RequiredClearanceResponse`

- [ ] **Step 1: 실패하는 MVC 테스트 작성**

`src/test/java/com/roomfit/product/controller/MockProductControllerTest.java`를 생성한다.

```java
package com.roomfit.product.controller;

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
class MockProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMockProducts_returnsAllMvpFurnitureTypes() throws Exception {
        mockMvc.perform(get("/api/products/mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[*].type").value(containsInAnyOrder(
                        "bed", "desk", "chair", "storage", "rug", "lamp"
                )))
                .andExpect(jsonPath("$.data[*].productId").value(hasItems(
                        "desk-01", "chair-01", "lamp-01"
                )))
                .andExpect(jsonPath("$.data[*].productId", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].name", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].brand", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].width", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].depth", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].height", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].price", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].styleTags", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].imageUrl", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].requiredClearance.front", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].requiredClearance.side", everyItem(notNullValue())));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run:

```bash
./gradlew test --tests com.roomfit.product.controller.MockProductControllerTest
```

Expected:

```text
BUILD FAILED
```

예상 실패 이유는 `404 Not Found` 또는 `No handler found` 계열이다. 아직 `GET /api/products/mock` controller가 없기 때문에 실패해야 한다.

- [ ] **Step 3: domain 모델 생성**

`src/main/java/com/roomfit/product/domain/RequiredClearance.java`를 생성한다.

```java
package com.roomfit.product.domain;

public class RequiredClearance {

    private final double front;
    private final double side;

    public RequiredClearance(double front, double side) {
        this.front = front;
        this.side = side;
    }

    public double getFront() {
        return front;
    }

    public double getSide() {
        return side;
    }
}
```

`src/main/java/com/roomfit/product/domain/MockProduct.java`를 생성한다.

```java
package com.roomfit.product.domain;

import java.util.List;

public class MockProduct {

    private final String productId;
    private final String type;
    private final String name;
    private final String brand;
    private final double width;
    private final double depth;
    private final double height;
    private final int price;
    private final List<String> styleTags;
    private final String imageUrl;
    private final RequiredClearance requiredClearance;

    public MockProduct(String productId, String type, String name, String brand,
                       double width, double depth, double height, int price,
                       List<String> styleTags, String imageUrl,
                       RequiredClearance requiredClearance) {
        this.productId = productId;
        this.type = type;
        this.name = name;
        this.brand = brand;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.price = price;
        this.styleTags = List.copyOf(styleTags);
        this.imageUrl = imageUrl;
        this.requiredClearance = requiredClearance;
    }

    public String getProductId() {
        return productId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public double getWidth() {
        return width;
    }

    public double getDepth() {
        return depth;
    }

    public double getHeight() {
        return height;
    }

    public int getPrice() {
        return price;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public RequiredClearance getRequiredClearance() {
        return requiredClearance;
    }
}
```

- [ ] **Step 4: response DTO 생성**

`src/main/java/com/roomfit/product/dto/response/RequiredClearanceResponse.java`를 생성한다.

```java
package com.roomfit.product.dto.response;

import com.roomfit.product.domain.RequiredClearance;

public class RequiredClearanceResponse {

    private final double front;
    private final double side;

    private RequiredClearanceResponse(double front, double side) {
        this.front = front;
        this.side = side;
    }

    public static RequiredClearanceResponse from(RequiredClearance requiredClearance) {
        return new RequiredClearanceResponse(requiredClearance.getFront(), requiredClearance.getSide());
    }

    public double getFront() {
        return front;
    }

    public double getSide() {
        return side;
    }
}
```

`src/main/java/com/roomfit/product/dto/response/MockProductResponse.java`를 생성한다.

```java
package com.roomfit.product.dto.response;

import com.roomfit.product.domain.MockProduct;

import java.util.List;

public class MockProductResponse {

    private final String productId;
    private final String type;
    private final String name;
    private final String brand;
    private final double width;
    private final double depth;
    private final double height;
    private final int price;
    private final List<String> styleTags;
    private final String imageUrl;
    private final RequiredClearanceResponse requiredClearance;

    private MockProductResponse(String productId, String type, String name, String brand,
                                double width, double depth, double height, int price,
                                List<String> styleTags, String imageUrl,
                                RequiredClearanceResponse requiredClearance) {
        this.productId = productId;
        this.type = type;
        this.name = name;
        this.brand = brand;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.price = price;
        this.styleTags = styleTags;
        this.imageUrl = imageUrl;
        this.requiredClearance = requiredClearance;
    }

    public static MockProductResponse from(MockProduct product) {
        return new MockProductResponse(
                product.getProductId(),
                product.getType(),
                product.getName(),
                product.getBrand(),
                product.getWidth(),
                product.getDepth(),
                product.getHeight(),
                product.getPrice(),
                product.getStyleTags(),
                product.getImageUrl(),
                RequiredClearanceResponse.from(product.getRequiredClearance())
        );
    }

    public String getProductId() {
        return productId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public double getWidth() {
        return width;
    }

    public double getDepth() {
        return depth;
    }

    public double getHeight() {
        return height;
    }

    public int getPrice() {
        return price;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public RequiredClearanceResponse getRequiredClearance() {
        return requiredClearance;
    }
}
```

- [ ] **Step 5: Repository 생성**

`src/main/java/com/roomfit/product/repository/MockProductRepository.java`를 생성한다.

```java
package com.roomfit.product.repository;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MockProductRepository {

    private final List<MockProduct> products = List.of(
            new MockProduct(
                    "bed-01",
                    "bed",
                    "화이트 싱글 침대",
                    "RoomFit Mock",
                    1.1,
                    2.0,
                    0.45,
                    129000,
                    List.of("minimal", "white_tone", "relax"),
                    "/images/products/bed-white.png",
                    new RequiredClearance(0.5, 0.2)
            ),
            new MockProduct(
                    "desk-01",
                    "desk",
                    "화이트 미니멀 책상",
                    "RoomFit Mock",
                    1.2,
                    0.6,
                    0.72,
                    89000,
                    List.of("minimal", "white_tone", "study"),
                    "/images/products/desk-white.png",
                    new RequiredClearance(0.6, 0.2)
            ),
            new MockProduct(
                    "chair-01",
                    "chair",
                    "화이트 기본 의자",
                    "RoomFit Mock",
                    0.45,
                    0.45,
                    0.8,
                    39000,
                    List.of("minimal", "white_tone", "study"),
                    "/images/products/chair-white.png",
                    new RequiredClearance(0.4, 0.1)
            ),
            new MockProduct(
                    "storage-01",
                    "storage",
                    "우드 수납장",
                    "RoomFit Mock",
                    0.8,
                    0.4,
                    1.2,
                    79000,
                    List.of("natural", "wood_tone", "storage"),
                    "/images/products/storage-wood.png",
                    new RequiredClearance(0.5, 0.2)
            ),
            new MockProduct(
                    "rug-01",
                    "rug",
                    "코지 원형 러그",
                    "RoomFit Mock",
                    1.2,
                    1.2,
                    0.02,
                    29000,
                    List.of("cozy", "natural", "open_space"),
                    "/images/products/rug-cozy.png",
                    new RequiredClearance(0.1, 0.1)
            ),
            new MockProduct(
                    "lamp-01",
                    "lamp",
                    "미니멀 스탠드 조명",
                    "RoomFit Mock",
                    0.25,
                    0.25,
                    1.2,
                    29000,
                    List.of("minimal", "cozy", "study"),
                    "/images/products/lamp-minimal.png",
                    new RequiredClearance(0.2, 0.1)
            )
    );

    public List<MockProduct> findAll() {
        return products;
    }
}
```

- [ ] **Step 6: Service 생성**

`src/main/java/com/roomfit/product/service/MockProductService.java`를 생성한다.

```java
package com.roomfit.product.service;

import com.roomfit.product.dto.response.MockProductResponse;
import com.roomfit.product.repository.MockProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockProductService {

    private final MockProductRepository mockProductRepository;

    public MockProductService(MockProductRepository mockProductRepository) {
        this.mockProductRepository = mockProductRepository;
    }

    public List<MockProductResponse> getMockProducts() {
        return mockProductRepository.findAll().stream()
                .map(MockProductResponse::from)
                .toList();
    }
}
```

- [ ] **Step 7: Controller 생성**

`src/main/java/com/roomfit/product/controller/MockProductController.java`를 생성한다.

```java
package com.roomfit.product.controller;

import com.roomfit.common.CommonResponse;
import com.roomfit.product.dto.response.MockProductResponse;
import com.roomfit.product.service.MockProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class MockProductController {

    private final MockProductService mockProductService;

    public MockProductController(MockProductService mockProductService) {
        this.mockProductService = mockProductService;
    }

    @GetMapping("/mock")
    public CommonResponse<List<MockProductResponse>> getMockProducts() {
        return CommonResponse.ok(mockProductService.getMockProducts());
    }
}
```

- [ ] **Step 8: 대상 테스트 통과 확인**

Run:

```bash
./gradlew test --tests com.roomfit.product.controller.MockProductControllerTest
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
git diff -- src/main/java/com/roomfit/product src/test/java/com/roomfit/product
```

검토 기준:

- `GET /api/products/mock` 외 endpoint가 추가되지 않았다.
- 응답 필드는 명세서 필드만 포함한다.
- `CommonResponse.ok(data)`를 사용한다.
- `ErrorCode`를 수정하지 않았다.
- `room`, `agent`, `placement` 패키지를 수정하지 않았다.
- 테스트가 6개 타입과 `desk-01`, `chair-01`, `lamp-01`을 확인한다.

- [ ] **Step 11: 커밋**

Run:

```bash
git add src/main/java/com/roomfit/product src/test/java/com/roomfit/product
git commit -m "feat: add mock products api"
```

Expected:

```text
[develop <commit>] feat: add mock products api
```

---

## 계획 자체 검토

- Spec coverage: `GET /api/products/mock`, 6개 MVP 타입, `CommonResponse`, 명세 필드, 참고 저장소형 패키지 구조, 테스트 요구사항을 Task 1에 반영했다.
- Placeholder scan: 빈칸으로 남긴 구현 지시나 모호한 후속 작업 표현을 남기지 않았다.
- Type consistency: `MockProduct`, `RequiredClearance`, `MockProductResponse`, `RequiredClearanceResponse`, `MockProductRepository`, `MockProductService`, `MockProductController`의 메서드명과 반환 타입은 각 단계에서 일관되게 사용한다.
