package com.roomfit.room;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double width;
    private double depth;
    private double height;
    private String unit; // meter 고정
    // @OrderColumn을 붙여 인덱스 기반 List로 매핑 — 붙이지 않으면 Hibernate가
    // "bag" semantics로 처리해 dirty-checking 시 원소(embeddable)의 hashCode를
    // 계산하려 드는데, Furniture.styleTags처럼 @Convert된 List<String> 속성이
    // 있으면 그 계산 경로에서 UnsupportedOperationException이 난다.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_walls", joinColumns = @JoinColumn(name = "room_id"))
    @OrderColumn(name = "wall_order")
    private List<Wall> walls;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_openings", joinColumns = @JoinColumn(name = "room_id"))
    @OrderColumn(name = "opening_order")
    private List<Opening> openings;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_furniture", joinColumns = @JoinColumn(name = "room_id"))
    @OrderColumn(name = "furniture_order")
    @Fetch(FetchMode.SELECT)
    private List<Furniture> furniture;
    @Enumerated(EnumType.STRING)
    private RoomSource source;
    private LocalDateTime createdAt;
    // Base64-encoded JPEG snapshot taken by the iOS app at scan completion
    // (RoomCaptureViewContainer's snapshotImage()). Stored inline alongside
    // the rest of the room JSON rather than as a separate file/object-storage
    // upload, matching this repo's existing in-memory-map storage model.
    // Null for sample rooms and any older upload predating this field.
    @Lob
    private String thumbnailBase64;

    protected Room() {
        // JPA/JSON 역직렬화용
    }

    public Room(Long id, double width, double depth, double height, String unit,
                List<Opening> openings, List<Furniture> furniture) {
        this(id, "Sample Room", width, depth, height, unit, List.of(), openings, furniture,
                RoomSource.SAMPLE, LocalDateTime.now(), null);
    }

    public Room(Long id, String name, double width, double depth, double height, String unit,
                List<Wall> walls, List<Opening> openings, List<Furniture> furniture, RoomSource source,
                LocalDateTime createdAt, String thumbnailBase64) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.unit = unit;
        // Hibernate가 @ElementCollection 필드를 직접 관리하려면 가변 List여야
        // 한다 — List.of()/List.copyOf()로 만든 불변 리스트를 넣으면 저장 시
        // UnsupportedOperationException이 난다.
        this.walls = walls == null ? new ArrayList<>() : new ArrayList<>(walls);
        this.openings = openings == null ? new ArrayList<>() : new ArrayList<>(openings);
        this.furniture = furniture == null ? new ArrayList<>() : new ArrayList<>(furniture);
        this.source = source == null ? RoomSource.SAMPLE : source;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.thumbnailBase64 = thumbnailBase64;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
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

    public String getUnit() {
        return unit;
    }

    public List<Wall> getWalls() {
        return walls;
    }

    public List<Opening> getOpenings() {
        return openings;
    }

    public List<Furniture> getFurniture() {
        return furniture;
    }

    // 확정(confirm) 시 Layout의 최종 가구 배치를 Room에 되반영하기 위해 필요
    // (LayoutService.confirmLayout 참고). manage-furniture 단계의 전체 배치
    // 교체(RoomService.replaceFurniture)에도 사용.
    public void setFurniture(List<Furniture> furniture) {
        this.furniture = furniture == null ? new ArrayList<>() : new ArrayList<>(furniture);
    }

    public RoomSource getSource() {
        return source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getThumbnailBase64() {
        return thumbnailBase64;
    }
}
