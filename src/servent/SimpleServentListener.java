package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.ab.ABAckHandler;
import servent.handler.snapshot.ab.ABTokenHandler;
import servent.handler.snapshot.av.AVDoneHandler;
import servent.handler.snapshot.av.AVTerminateHandler;
import servent.handler.snapshot.av.AVTokenHandler;
import servent.handler.snapshot.cc.CCAckHandler;
import servent.handler.snapshot.cc.CCResumeHandler;
import servent.handler.snapshot.cc.CCSnapshotRequestHandler;
import servent.message.Message;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	private SnapshotCollector snapshotCollector;
	
	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	private List<Message> redMessages = new ArrayList<>();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;

				/*
				 * This blocks for up to 1s, after which SocketTimeoutException is thrown.
				 */
				Socket clientSocket = listenerSocket.accept();

				//GOT A MESSAGE! <3
				clientMessage = MessageUtil.readMessage(clientSocket);


				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
					case TRANSACTION:
						messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
						break;

					case CC_SNAPSHOT_REQUEST:
						messageHandler = new CCSnapshotRequestHandler(clientMessage, snapshotCollector);
						break;
					case CC_ACK:
						messageHandler = new CCAckHandler(clientMessage, (SnapshotCollectorWorker)snapshotCollector);
						break;
					case CC_RESUME:
						messageHandler = new CCResumeHandler(clientMessage, snapshotCollector);
						break;

					case ACHARYA_BADRINATH_ASK_AMOUNT:
						messageHandler = new ABTokenHandler(clientMessage, snapshotCollector);
						break;
					case ACHARYA_BADRINATH_TELL_AMOUNT:
						messageHandler = new ABAckHandler(clientMessage, snapshotCollector);
						break;
					case AV_TOKEN:
						messageHandler = new AVTokenHandler(clientMessage, snapshotCollector);
						break;
					case AV_DONE:
						messageHandler = new AVDoneHandler(clientMessage, (SnapshotCollectorWorker)snapshotCollector);
						break;
					case AV_TERMINATE:
						messageHandler = new AVTerminateHandler(clientMessage, (SnapshotCollectorWorker)snapshotCollector);
						break;
					case POISON:
						break;
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
