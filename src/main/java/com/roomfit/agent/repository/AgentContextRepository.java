package com.roomfit.agent.repository;

import com.roomfit.agent.domain.AgentContext;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentContextRepository extends JpaRepository<AgentContext, Long> {
}
