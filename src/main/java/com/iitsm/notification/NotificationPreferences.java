package com.iitsm.notification;

import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.user.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Which channels each role receives, configurable in application.yml, e.g.
 * <pre>
 * itsm.notifications.channels.TECHNICIAN: IN_APP,EMAIL,SMS
 * </pre>
 * Defaults: everyone gets in-app and email; technicians and administrators also get SMS for urgent
 * messages.
 */
@Component
@ConfigurationProperties(prefix = "itsm.notifications")
public class NotificationPreferences {

    private final Map<Role, Set<ChannelType>> channels = new EnumMap<>(Role.class);

    public NotificationPreferences() {
        channels.put(Role.EMPLOYEE, EnumSet.of(ChannelType.IN_APP, ChannelType.EMAIL));
        channels.put(Role.TECHNICIAN, EnumSet.of(ChannelType.IN_APP, ChannelType.EMAIL, ChannelType.SMS));
        channels.put(Role.ADMIN, EnumSet.of(ChannelType.IN_APP, ChannelType.EMAIL, ChannelType.SMS));
    }

    /** Bound from configuration (Spring calls this getter and fills the map). */
    public Map<Role, Set<ChannelType>> getChannels() {
        return channels;
    }

    public Set<ChannelType> channelsFor(Role role) {
        return channels.getOrDefault(role, EnumSet.of(ChannelType.IN_APP));
    }
}
