package com.roomfit.agent;

import com.roomfit.agent.dto.AgentContextRequest;
import com.roomfit.agent.dto.AgentContextResponse;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.RoomRepository;
import org.springframework.stereotype.Service;

@Service
public class AgentContextService {

    private final AgentContextRepository agentContextRepository;
    private final RoomRepository roomRepository;

    public AgentContextService(AgentContextRepository agentContextRepository, RoomRepository roomRepository) {
        this.agentContextRepository = agentContextRepository;
        this.roomRepository = roomRepository;
    }

    public AgentContextResponse createContext(AgentContextRequest request) {
        roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (request.getRequiredItems() == null || request.getRequiredItems().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_ITEM_EMPTY);
        }

        LifestyleGoal lifestyleGoal = LifestyleGoal.valueOf(request.getLifestyleGoal().toUpperCase());

        AgentContext context = new AgentContext(
                request.getRoomId(),
                lifestyleGoal,
                request.getDesignStyle(),
                request.getRequiredItems(),
                request.getOptionalItems(),
                request.getStyleTags()
        );

        agentContextRepository.save(context);
        return AgentContextResponse.from(context);
    }
}
