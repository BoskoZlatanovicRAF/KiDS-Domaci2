package app.snapshot_bitcake.av_alagar_venkatesan;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.BitcakeManager;
import servent.message.Message;
import servent.message.TransactionMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AVBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(1000);


    private Map<Integer, List<Integer>> channelStates = new ConcurrentHashMap<>();
    private volatile Map<Integer, Integer> snapshotVector = null;
    private final int myId = AppConfig.myServentInfo.getId();

    @Override
    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }


    public void startLogging(Map<Integer, Integer> snapshotClock) {
        this.snapshotVector = snapshotClock;
        channelStates.clear(); // one list per sender
    }

    public void stopLogging() {
        this.snapshotVector = null;
    }

    // Call this from TransactionHandler
    public void logIfOld(Message msg) {
        if (snapshotVector == null || msg.getOriginalSenderInfo().getId() == myId) return;

        Map<Integer, Integer> messageClock = ((TransactionMessage) msg).getSenderVectorClock();
        if (!CausalBroadcastShared.otherClockGreater(messageClock, snapshotVector)) {
            int from = msg.getOriginalSenderInfo().getId();
            int amount = Integer.parseInt(msg.getMessageText());

            channelStates.computeIfAbsent(from, k -> new ArrayList<>()).add(amount);
        }
    }

    public Map<Integer, List<Integer>> getChannelStates() {
        return channelStates;
    }

}
