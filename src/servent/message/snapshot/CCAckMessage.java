package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class CCAckMessage extends BasicMessage {

    private static final long serialVersionUID = 31465658645342313L;

    public CCAckMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, String message) {
        super(MessageType.CC_ACK, originalSenderInfo, receiverInfo, message);
    }
}
