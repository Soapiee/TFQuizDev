package me.soapiee.common.versionsupport;

import me.soapiee.common.utils.Logger;
import org.bukkit.block.Sign;

public class Version_Unsupported implements VersionProvider {

    private final Logger logger;

    public Version_Unsupported(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void setLine(Sign sign, int lineNo, String text) {
        logger.logToFile(null, "Game signs are unsupported on this version. Please remove them");
    }
}
