package app.snapshot_bitcake;

import java.io.Serializable;

public class CCSnapshotResult implements Serializable {
    private static final long serialVersionUID = 97043423543451L;

    private final int serventId;
    private final int recordedAmount;

    public CCSnapshotResult(int serventId, int recordedAmount) {
        this.serventId = serventId;
        this.recordedAmount = recordedAmount;
    }
    
    public int getServentId() {
        return serventId;
    }
    
    public int getRecordedAmount() {
        return recordedAmount;
    }
}
