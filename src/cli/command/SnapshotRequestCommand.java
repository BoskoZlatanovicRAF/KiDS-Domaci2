package cli.command;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollectorWorker;

public class SnapshotRequestCommand implements CLICommand{
    @Override
    public String commandName() {
        return "snapshot_request";
    }

    @Override
    public void execute(String args) {

    }
}
