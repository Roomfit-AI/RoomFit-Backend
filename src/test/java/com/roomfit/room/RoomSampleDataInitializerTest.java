package com.roomfit.room;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class RoomSampleDataInitializerTest {

    @Test
    void repeatedRunsCreateExactlyOneCanonicalSample() throws Exception {
        RoomRepository repository = mock(RoomRepository.class);
        AtomicReference<Room> saved = new AtomicReference<>();
        when(repository.findBySourceOrderByIdAsc(RoomSource.SAMPLE))
                .thenAnswer(invocation -> saved.get() == null ? List.of() : List.of(saved.get()));
        when(repository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            saved.set(room);
            return room;
        });
        RoomSampleDataInitializer initializer = new RoomSampleDataInitializer(repository);

        initializer.run();
        initializer.run();

        verify(repository, times(1)).save(any(Room.class));
        assertThat(saved.get()).satisfies(room -> {
            assertThat(RoomSampleDataInitializer.isCanonicalSample(room)).isTrue();
            assertThat(room.getFurniture()).extracting(Furniture::getId)
                    .containsExactly("bed-1", "desk-1", "chair-1", "wardrobe-1");
        });
    }

    @Test
    void existingCanonicalSampleUpdatesOnlyAnOutdatedWardrobeAndThenBecomesANoOp() throws Exception {
        RoomRepository repository = mock(RoomRepository.class);
        Room canonical = canonicalSampleWithWardrobe(new Position(5.0, 3.85), 180);
        when(repository.findBySourceOrderByIdAsc(RoomSource.SAMPLE)).thenReturn(List.of(canonical));
        RoomSampleDataInitializer initializer = new RoomSampleDataInitializer(repository);

        initializer.run();
        initializer.run();

        Furniture wardrobe = canonical.getFurniture().stream()
                .filter(item -> "wardrobe-1".equals(item.getId())).findFirst().orElseThrow();
        assertThat(wardrobe.getPosition().getX()).isEqualTo(5.39);
        assertThat(wardrobe.getPosition().getZ()).isEqualTo(3.85);
        assertThat(wardrobe.getRotation()).isEqualTo(90);
        verify(repository, times(1)).save(canonical);
    }

    private Room canonicalSampleWithWardrobe(Position position, double rotation) {
        Room sample = new Room(null, 5.8, 5.4, 2.7, "meter", List.of(), List.of(
                new Furniture("bed-1", "bed", "침대", 1, 2, 0.5, new Position(1, 1), 0, FurnitureStatus.EXISTING),
                new Furniture("desk-1", "desk", "책상", 1, 0.6, 0.7, new Position(3, 1), 0, FurnitureStatus.EXISTING),
                new Furniture("chair-1", "chair", "의자", 0.5, 0.5, 0.8, new Position(3, 2), 0, FurnitureStatus.EXISTING),
                new Furniture("wardrobe-1", "wardrobe", "옷장", 1.2, 0.65, 2.1, position, rotation,
                        FurnitureStatus.EXISTING)));
        return sample;
    }
}
