package io.takari.bpm.event;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class ExpiredEvent implements Serializable {
	
	private static final long serialVersionUID = 1L;

    private final UUID id;
    private final Date expiredAt;

    public ExpiredEvent(UUID id, Date expiredAt) {
        this.id = id;
        this.expiredAt = expiredAt;
    }

    public UUID geId() {
        return id;
    }

    public Date getExpiredAt() {
        return expiredAt;
    }
}
