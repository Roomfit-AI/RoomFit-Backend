package com.roomfit.room;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 서버 기동 시 AI 추천 전 데모에 사용하는 샘플 원룸을 시딩한다.
 */
@Component
public class RoomSampleDataInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;

    public RoomSampleDataInitializer(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public void run(String... args) {
        Opening door = new Opening("door-1", "door", "south", 4.6, 0.8, 2.1, null);
        Opening window = new Opening("window-1", "window", "north", 0.65, 2.2, 1.5, 0.7);

        Furniture bed = new Furniture("bed-1", "bed", "우드 침대", 1.45, 2.1, 0.48,
                new Position(1.35, 1.55), 0, FurnitureStatus.EXISTING);
        Furniture desk = new Furniture("desk-1", "desk", "우드 책상", 1.35, 0.6, 0.72,
                new Position(3.0, 1.05), 0, FurnitureStatus.EXISTING);
        Furniture chair = new Furniture("chair-1", "chair", "우드 의자", 0.55, 0.55, 0.82,
                new Position(3.0, 1.85), 180, FurnitureStatus.EXISTING);
        Furniture wardrobe = new Furniture("wardrobe-1", "storage", "우드 옷장", 1.2, 0.65, 2.1,
                new Position(5.0, 3.85), 180, FurnitureStatus.EXISTING);

        Room sampleRoom = new Room(null, 5.8, 5.4, 2.7, "meter",
                List.of(door, window), List.of(bed, desk, chair, wardrobe));

        roomRepository.save(sampleRoom); // roomId = 1 로 발급됨

        Opening collectorDoor = new Opening("collector-door", "door", "south", 5.7, 0.8, 2.1, null);
        Opening collectorWindow = new Opening("collector-window", "window", "north", 1.7, 2.4, 1.45, 0.72);

        List<Furniture> collectorFurniture = List.of(
                new Furniture("collector-bed", "bed", "미드센추리 싱글 침대", 1.35, 2.0, 0.5,
                        new Position(1.35, 1.8), 0, FurnitureStatus.EXISTING),
                new Furniture("collector-bedside", "storage", "코랄 협탁", 0.48, 0.42, 0.52,
                        new Position(0.55, 1.7), 0, FurnitureStatus.EXISTING),
                new Furniture("collector-floor-plant", "storage", "플로어 식물", 0.48, 0.48, 0.92,
                        new Position(0.58, 4.5), 0, FurnitureStatus.EXISTING),
                new Furniture("collector-desk", "desk", "미드센추리 컬렉터 데스크", 1.35, 0.62, 0.74,
                        new Position(2.9, 1.0), 0, FurnitureStatus.EXISTING),
                new Furniture("collector-desk-chair", "chair", "월넛 데스크 체어", 0.58, 0.58, 0.82,
                        new Position(2.9, 1.95), 180, FurnitureStatus.EXISTING),
                new Furniture("collector-blue-cabinet", "storage", "코발트 모듈 수납장", 0.78, 0.42, 1.08,
                        new Position(1.35, 3.15), 0, FurnitureStatus.EXISTING),
                new Furniture("collector-glass-shelf", "storage", "크롬 글라스 전시 선반", 1.28, 0.36, 1.48,
                        new Position(3.95, 0.98), 0, FurnitureStatus.EXISTING),
                new Furniture("collector-console", "storage", "크림 LP 콘솔", 1.76, 0.44, 0.78,
                        new Position(5.08, 2.7), 0, FurnitureStatus.EXISTING),
                new Furniture("collector-red-shelf", "shelf", "레트로 레드 벽 선반", 0.92, 0.18, 0.22,
                        new Position(5.9, 2.3), 90, FurnitureStatus.EXISTING),
                new Furniture("collector-lounge-chair", "sofa", "코랄 라운지 체어", 0.92, 0.86, 0.84,
                        new Position(5.1, 4.55), 135, FurnitureStatus.EXISTING),
                new Furniture("collector-cane-chair", "chair", "케인 크롬 체어", 0.68, 0.7, 0.82,
                        new Position(3.75, 4.6), 320, FurnitureStatus.EXISTING),
                new Furniture("collector-rug", "rug", "크림 라운드 러그", 2.2, 2.2, 0.035,
                        new Position(3.9, 4.08), 0, FurnitureStatus.EXISTING),
                new Furniture("collector-coffee-table", "table", "글라스 커피 테이블", 0.9, 0.9, 0.42,
                        new Position(3.9, 4.08), 0, FurnitureStatus.EXISTING)
        );

        Room collectorRoom = new Room(null, "미드센추리 컬렉터 룸", 6.4, 5.8, 2.8, "meter",
                List.of(), List.of(collectorDoor, collectorWindow), collectorFurniture,
                RoomSource.SAMPLE, LocalDateTime.now());

        roomRepository.save(collectorRoom); // roomId = 2 로 발급됨
    }
}
