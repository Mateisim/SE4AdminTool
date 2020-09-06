package tfa.se4.steam;

import java.io.IOException;

import org.apache.http.client.fluent.Request;

import tfa.se4.steam.json.JSONUtils;
import tfa.se4.steam.json.Player;
import tfa.se4.steam.json.PlayerBansQueryResult;

import tfa.se4.logger.LoggerInterface;
import static tfa.se4.logger.LoggerInterface.LogLevel;
import static tfa.se4.logger.LoggerInterface.LogType;
/**
 * Holder class for anything we use to talk to Steam.
 */
public final class SteamAPI
{
    private String m_steamAPIkey;
    
    /**
     * Initialise and remember steam API key.
     * @param apiKey Steam API key.
     */
    public SteamAPI(final String apiKey)
    {
        m_steamAPIkey = apiKey;
    }
    
    /**
     * Get player ban information.
     * @param steamID SteamId of player to query
     * @param logger Logger for catching errors
     * @return Ban information or null if we were not able to fetch it.
     */
    public Player getBanInfo(final String steamID, final String playerName, final LoggerInterface logger)
    {
        logger.log(LogLevel.INFO, LogType.STEAM, "Checking player %s steam ID %s for bans", playerName, steamID);
        final String urltemplate = "http://api.steampowered.com/ISteamUser/GetPlayerBans/v1/?key=##STEAMAPIKEY##&steamids=##STEAMID##";
        
        final String url = urltemplate
                .replace("##STEAMID##", steamID)
                .replace("##STEAMAPIKEY##", m_steamAPIkey);
        
        try
        {
            final PlayerBansQueryResult bans = JSONUtils.unMarshalServerStatus(
                    Request.Get(url).connectTimeout(2000).socketTimeout(2000).execute().returnContent().asString(), logger);
            
            if (bans == null || bans.getPlayers() == null || bans.getPlayers().isEmpty())
            {
                logger.log(LogLevel.ERROR, LogType.STEAM, "Unable to fetch steam ban info for %s empty return data", steamID);
            }
            return bans.getPlayers().get(0);
        }
        catch (IOException e)
        {
            logger.log(LogLevel.ERROR, LogType.STEAM, e,"Unable to fetch steam ban info for %s", steamID);
            return null;
        }
    }
}
