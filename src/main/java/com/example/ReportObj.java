package com.example;

import net.lightbody.bmp.core.har.HarRequest;
import org.openqa.selenium.logging.LogEntries;

import java.util.ArrayList;
import java.util.List;

public class ReportObj {
    private List<HarRequest> requests;
    private LogEntries logEntries;

    public ReportObj(){
        requests = new ArrayList<>();
    }

    public List<HarRequest> getRequestList(){
        return requests;
    }
    public void setLogEntries(LogEntries logEntries){
        this.logEntries = logEntries;
    }
}
