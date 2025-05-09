package servent.message.snapshot.cc;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class CCSnapshotRequestMessage extends BasicMessage {

    private static final long serialVersionUID = -2881842484848283L;

    public CCSnapshotRequestMessage(ServentInfo sender, ServentInfo receiver) {
        super(MessageType.CC_SNAPSHOT_REQUEST, sender, receiver);
    }
}
