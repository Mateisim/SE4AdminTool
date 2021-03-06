package tfa.se4.gui;

import javafx.application.Platform;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import tfa.se4.Options;
import tfa.se4.SEAdminServerConnection;
import tfa.se4.json.ServerStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@WebSocket
public class MonitoredServerConnection extends SEAdminServerConnection
{

    private GameModel model;

    /**
     * Set up and manage connection based on properties configuration.
     *
     * @param options Configuration options.
     */
    public MonitoredServerConnection(Options options)
    {
        super(options);
        model = new GameModel();
        if (!isSteamSupported())
        {
            log(LogLevel.INFO, LogType.STEAM, "Steam access is disabled. No automatic ban checks will be performed.");
        }

        if (!ipStackSupported())
        {
            log(LogLevel.INFO, LogType.IPSTACK, "IPStack access is disabled. No location lookups will be performed.");
        }
    }

    @Override
    public void setServerStatus(ServerStatus status)
    {
        super.setServerStatus(status);

        Platform.runLater(() -> {
            if (status != null && status.getGameData() != null && status.getGameData().getCurrentMap() != null)
            {
                model.setMap(status.getGameData().getCurrentMap().getName());
                model.setMode(status.getGameData().getCurrentMap().getMode());
                model.setScoreLimit(status.getGameData().getCurrentMap().getScoreLimit().toString());
                model.setTimeLimit(status.getGameData().getCurrentMap().getTimeLimit().toString());
            }

            if (status != null && status.getLobby() != null)
            {
                model.setState(status.getLobby().getState());
                model.setMaxPlayers(status.getLobby().getMaxPlayers().toString());
                if (getIPStackAPI() != null)
                {
                    status.getLobby().getPlayers().forEach(p -> p.setLocation(getIPStackAPI().getLocation(p.getIPv4(), this)));
                }

                if (getSteamAPI() != null)
                {
                    status.getLobby().getPlayers().forEach(p -> p.setPlayhours(getSteamAPI().getTotalPlaytimeHours(p.getSteamId(), p.getName(), this)));
                }
                model.setPlayers(status.getLobby().getPlayers());
            }

            if (status != null && status.getServer() != null)
            {
                model.setServerName(status.getServer().getName());
                model.setServerHost(status.getServer().getHost());
            }

            if (getGameStartTime() == 0)
            {
                model.setTimeLeft(null);
            }
            else
            {
                final long endTime = getGameStartTime() + status.getGameData().getCurrentMap().getTimeLimit() * 60000;
                model.setTimeLeft(formatTimeLeft(endTime - System.currentTimeMillis()));
            }
        });
    }

    @Override
    public void log(LogLevel level, LogType type, String message, Object... args)
    {

        log(level, type, null, message, args);
    }

    @Override
    public void log(LogLevel level, LogType type, Throwable t, String message, Object... args)
    {
        if (isFilterMessage(level))
        {
            return;
        }

        final StringBuilder sb = new StringBuilder(128);
        sb.append(Instant.now().toString());
        sb.append('|');
        sb.append(level.label);
        sb.append('|');
        sb.append(type.label);
        sb.append('|');
        sb.append(String.format(message, args));

        if (t != null)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            sb.append('\n');
            sb.append(sw.toString());
        }

        Platform.runLater(() -> model.addLogLine(sb.toString()));
    }

    @Override
    public void updateServerStatistics(long bytesSent, long bytesReceived, float fps)
    {
        super.updateServerStatistics(bytesSent, bytesReceived, fps);

        Platform.runLater(() -> {
            model.setBytesSent(formatBytes(bytesSent));
            model.setBytesReceived(formatBytes(bytesReceived));
            model.setFps(Integer.toString(Math.round(fps)));

        });
    }

    /**
     * Is the IP stack in use ?
     *
     * @return Whether IP stack is in use.
     */
    public boolean ipStackSupported()
    {
        return getIPStackAPI() != null;

    }

    /**
     * Is steam in use ?
     *
     * @return Whether steam is in use
     */
    public boolean isSteamSupported()
    {
        return getSteamAPI() != null;
    }

    /**
     * Pretty print bytes values.
     *
     * @param bytes bytes
     * @return pretty printed bytes
     */
    private String formatBytes(final long bytes)
    {
        if (bytes < 1024)
        {
            return String.format("%d byte", bytes);
        }
        else if (bytes < (1024 * 1024))
        {
            return String.format("%.2f kb", (float) bytes / 1024.0);
        }
        else if (bytes < (1024 * 1024 * 1024))
        {
            return String.format("%.2f mb", (float) bytes / (1024.0 * 1024.0));
        }
        else
        {
            return String.format("%.2f gb", (float) bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private String formatTimeLeft(final long millis)
    {
        final long hours = TimeUnit.MILLISECONDS.toHours(millis);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        final long secs = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        if (hours > 0)
        {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
        else if (minutes > 0)
        {
            return String.format("%02d:%02d", minutes, secs);
        }
        else
        {
            return String.format("%02d", secs);
        }
    }

    public GameModel getModel()
    {
        return model;
    }
}
