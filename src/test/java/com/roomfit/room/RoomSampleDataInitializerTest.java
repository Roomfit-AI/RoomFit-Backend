package com.roomfit.room;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
}
