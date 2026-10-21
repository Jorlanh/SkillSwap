package br.com.teamss.skillswap.skill_swap.events;

import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import org.springframework.context.ApplicationEvent;

public class ProposalCompletedEvent extends ApplicationEvent {
    
    private final Proposal proposal;

    public ProposalCompletedEvent(Object source, Proposal proposal) {
        super(source);
        this.proposal = proposal;
    }

    public Proposal getProposal() {
        return proposal;
    }
}