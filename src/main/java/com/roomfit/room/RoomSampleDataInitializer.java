package com.roomfit.room;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 서버 기동 시 AI 추천 전 데모에 사용하는 샘플 원룸을 시딩한다.
 */
@Component
public class RoomSampleDataInitializer implements CommandLineRunner {

    static final String CANONICAL_SAMPLE_NAME = "Sample Room";

    private final RoomRepository roomRepository;

    public RoomSampleDataInitializer(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public void run(String... args) {
        // 이름과 geometry를 canonical key로 사용한다. SAMPLE 행이 하나라도 있다는
        // 이유로 건너뛰면 legacy sample만 남은 DB에서 canonical sample이 복구되지
        // 않는다. 반대로 canonical이 이미 있으면 재시작해도 새 행을 만들지 않는다.
        if (roomRepository.findBySourceOrderByIdAsc(RoomSource.SAMPLE).stream()
                .anyMatch(RoomSampleDataInitializer::isCanonicalSample)) {
            return;
        }

        Opening door = new Opening("door-1", "door", "south", 4.6, 0.8, 2.1, null);
        Opening window = new Opening("window-1", "window", "north", 0.65, 2.2, 1.5, 0.7);

        Furniture bed = new Furniture("bed-1", "bed", "우드 침대", 1.45, 2.1, 0.48,
                new Position(1.35, 1.55), 0, FurnitureStatus.EXISTING);
        Furniture desk = new Furniture("desk-1", "desk", "우드 책상", 1.35, 0.6, 0.72,
                new Position(3.0, 1.05), 0, FurnitureStatus.EXISTING);
        Furniture chair = new Furniture("chair-1", "chair", "우드 의자", 0.55, 0.55, 0.82,
                new Position(3.0, 1.85), 180, FurnitureStatus.EXISTING);
        Furniture wardrobe = new Furniture("wardrobe-1", "wardrobe", "우드 옷장", 1.2, 0.65, 2.1,
                new Position(5.0, 3.85), 180, FurnitureStatus.EXISTING);

        Room sampleRoom = new Room(null, 5.8, 5.4, 2.7, "meter",
                List.of(door, window), List.of(bed, desk, chair, wardrobe));

        roomRepository.save(sampleRoom);
    }

    static boolean isCanonicalSample(Room room) {
        return room.getSource() == RoomSource.SAMPLE
                && CANONICAL_SAMPLE_NAME.equals(room.getName())
                && Double.compare(room.getWidth(), 5.8) == 0
                && Double.compare(room.getDepth(), 5.4) == 0;
    }
}
