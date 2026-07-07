package com.roomfit.agent;

import com.roomfit.agent.dto.AgentContextRequest;
import com.roomfit.agent.dto.AgentContextResponse;
import com.roomfit.common.CommonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentContextController {

    private final AgentContextService agentContextService;

    public AgentContextController(AgentContextService agentContextService) {
        this.agentContextService = agentContextService;
    }

    @PostMapping("/context")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<AgentContextResponse> createContext(@RequestBody AgentContextRequest request) {
        return CommonResponse.ok(agentContextService.createContext(request));
    }
}
