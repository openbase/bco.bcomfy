package org.openbase.bco.bcomfy.utils;

import rsb.Factory;
import rsb.config.ParticipantConfig;
import rsb.config.TransportConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class RSBDefaultConfig {

    private static boolean init = false;
    private static ParticipantConfig participantConfig;

    public synchronized static void load() {
        if (init) {
            return;
        }
        participantConfig = Factory.getInstance().getDefaultParticipantConfig();

        enableTransport(participantConfig);
        setupHost(participantConfig, "129.70.135.69");
        setupPort(participantConfig, 4803);

        init = true;
    }

    public static ParticipantConfig getDefaultParticipantConfig() {
        if (!init) {
            load();
        }
        return participantConfig;
    }

    public static void enableTransport(final ParticipantConfig participantConfig) {
        for (TransportConfig transport : participantConfig.getEnabledTransports()) {
            transport.setEnabled(false);
        }
        participantConfig.getOrCreateTransport("spread").setEnabled(true);
    }

    public static void setupHost(final ParticipantConfig participantConfig, final String host) {
        for (TransportConfig config : participantConfig.getTransports().values()) {

            if (!config.isEnabled()) {
                continue;
            }

            final String hostProperty = "transport." + config.getName() + ".host";

            // remove configured host
            if (config.getOptions().hasProperty(hostProperty)) {
                config.getOptions().remove(hostProperty);
            }

            // setup host
            config.getOptions().setProperty(hostProperty, host);

        }
    }

    public static void setupPort(final ParticipantConfig participantConfig, final Integer port) {
        for (TransportConfig config : participantConfig.getTransports().values()) {

            if (!config.isEnabled()) {
                continue;
            }

            final String portProperty = "transport." + config.getName() + ".port";

            // remove configured host
            if (config.getOptions().hasProperty(portProperty)) {
                config.getOptions().remove(portProperty);
            }

            // setup host
            config.getOptions().setProperty(portProperty, Integer.toString(port));
        }
    }
}
