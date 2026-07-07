package com.roomfit.room;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 서버 기동 시 샘플 원룸(3.2m x 4.5m, 침대/책상/옷장 기존 배치) 데이터를 시딩한다.
 * API 명세서 1-1 확정 내용 기준.
 */
@Component
public class RoomSampleDataInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;

    public RoomSampleDataInitializer(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public void run(String... args) {
        Opening door = new Opening("door-1", "door", "south", 0.7, 0.8, 2.1, null);
        Opening window = new Opening("window-1", "window", "north", 1.5, 1.2, 1.0, 0.9);

        Furniture bed = new Furniture("bed-1", "bed", "침대", 1.1, 2.0, 0.45,
                new Position(0.8, 1.4), 0, FurnitureStatus.EXISTING);
        Furniture desk = new Furniture("desk-1", "desk", "책상", 1.0, 0.5, 0.72,
                new Position(2.7, 0.4), 90, FurnitureStatus.EXISTING);
        Furniture wardrobe = new Furniture("wardrobe-1", "storage", "옷장", 0.9, 0.6, 1.8,
                new Position(2.7, 3.9), 0, FurnitureStatus.EXISTING);

        Room sampleRoom = new Room(null, 3.2, 4.5, 2.4, "meter",
                List.of(door, window), List.of(bed, desk, wardrobe));

        roomRepository.save(sampleRoom); // roomId = 1 로 발급됨
    }
}
