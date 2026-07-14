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

        Opening studioDoor = new Opening("studio-door", "door", "south", 5.7, 0.8, 2.1, null);
        Opening studioWindow = new Opening("studio-window", "window", "north", 1.7, 2.4, 1.45, 0.72);

        List<Furniture> studioFurniture = List.of(
                new Furniture("bed-3", "bed", "우드 침대", 1.45, 2.1, 0.48,
                        new Position(1.35, 1.55), 0, FurnitureStatus.EXISTING),
                new Furniture("desk-3", "desk", "우드 책상", 1.35, 0.6, 0.72,
                        new Position(3.0, 1.05), 0, FurnitureStatus.EXISTING),
                new Furniture("chair-3", "chair", "우드 의자", 0.55, 0.55, 0.82,
                        new Position(3.0, 1.85), 180, FurnitureStatus.EXISTING),
                new Furniture("wardrobe-3", "storage", "우드 옷장", 1.2, 0.65, 2.1,
                        new Position(5.3, 3.85), 180, FurnitureStatus.EXISTING)
        );

        Room studioRoom = new Room(null, "미드센추리 컬렉터 스튜디오", 6.4, 5.8, 2.8, "meter",
                List.of(), List.of(studioDoor, studioWindow), studioFurniture,
                RoomSource.SAMPLE, LocalDateTime.now(), null);

        roomRepository.save(studioRoom); // roomId = 2 로 발급됨
    }
}
