package com.academicpassport.marksheet.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MarksheetUploadedEvent extends ApplicationEvent {
    
    private final Long marksheetId;

    public MarksheetUploadedEvent(Object source, Long marksheetId) {
        super(source);
        this.marksheetId = marksheetId;
    }
}
