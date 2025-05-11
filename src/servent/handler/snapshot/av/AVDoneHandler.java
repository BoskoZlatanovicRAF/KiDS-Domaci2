package servent.handler.snapshot.av;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.av.AVTerminateMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AVDoneHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollectorWorker snapshotCollectorWorker;

    private static final Set<Integer> doneNodes = ConcurrentHashMap.newKeySet();


    public AVDoneHandler(Message clientMessage, SnapshotCollectorWorker snapshotCollectorWorker) {
        this.clientMessage = clientMessage;
        this.snapshotCollectorWorker = snapshotCollectorWorker;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.AV_DONE) {
            AppConfig.timestampedErrorPrint("Invalid message in AVDoneHandler: " + clientMessage);
            return;
        }

        int senderId = clientMessage.getOriginalSenderInfo().getId();
        doneNodes.add(senderId);

        AppConfig.timestampedStandardPrint("Received AV_DONE from " + senderId + ". Total received: " + doneNodes.size());

        if (doneNodes.size() == AppConfig.getServentCount() - 1) { // excluding self
            AppConfig.timestampedStandardPrint("All AV_DONEs received. Sending AV_TERMINATE to all...");

            for (int i = 0; i < AppConfig.getServentCount(); i++) {
                if (i == AppConfig.myServentInfo.getId()) continue; // no need to send to self

                ServentInfo receiver = AppConfig.getInfoById(i);
                Map<Integer, Integer> currentClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

                AVTerminateMessage terminateMessage = new AVTerminateMessage(MessageType.AV_TERMINATE, AppConfig.myServentInfo, receiver, currentClock);
                MessageUtil.sendMessage(terminateMessage);
            }

        }

    }

    public static void clearDoneNodes() {
        doneNodes.clear();
    }
}
