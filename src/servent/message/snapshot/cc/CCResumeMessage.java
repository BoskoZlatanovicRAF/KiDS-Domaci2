package servent.message.snapshot.cc;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class CCResumeMessage extends BasicMessage {

    private static final long serialVersionUID = -7684753423231455L;

    public CCResumeMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
        super(MessageType.CC_RESUME, originalSenderInfo, receiverInfo);
    }
}
